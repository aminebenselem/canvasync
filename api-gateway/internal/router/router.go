package router

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"
	"time"

	"github.com/aminebenselem/api-gateway/internal/config"
	"github.com/aminebenselem/api-gateway/internal/loadbalancer"
	"github.com/aminebenselem/api-gateway/internal/ratelimiter"
)

type Router struct {
	routes        *config.RoutesConfig
	loadBalancers map[string]*loadbalancer.LoadBalancer
	rateLimiter   *ratelimiter.RateLimiter
}

func New(routes *config.RoutesConfig) *Router {
	loadBalancers := make(map[string]*loadbalancer.LoadBalancer)

	for _, route := range routes.Routes {
		lb := loadbalancer.New(route.Targets)

		lb.StartHealthChecks(5 * time.Second)

		loadBalancers[route.Path] = lb
	}

	return &Router{
		routes:        routes,
		loadBalancers: loadBalancers,
		rateLimiter:   ratelimiter.New(5, 10),
	}
}

func (r *Router) Match(path string) (*config.Route, bool) {
	var matched *config.Route

	for i := range r.routes.Routes {
		route := &r.routes.Routes[i]

		if strings.HasPrefix(path, route.Path) {
			if matched == nil || len(route.Path) > len(matched.Path) {
				matched = route
			}
		}
	}

	if matched == nil {
		return nil, false
	}

	return matched, true
}

func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {

	// ---------------------------------------------------------
	// 1. Rate limiting
	// ---------------------------------------------------------

	if !r.rateLimiter.Allow() {
		http.Error(
			w,
			"rate limit exceeded",
			http.StatusTooManyRequests,
		)
		return
	}

	// ---------------------------------------------------------
	// 2. Find matching route
	// ---------------------------------------------------------

	route, ok := r.Match(req.URL.Path)

	if !ok {
		http.NotFound(w, req)
		return
	}

	// ---------------------------------------------------------
	// 3. Select a healthy backend
	//
	// IMPORTANT:
	// This happens ONCE for the request.
	//
	// For HTTP:
	//   request -> backend
	//
	// For WebSocket:
	//   handshake -> backend
	//   upgraded connection stays with that backend
	// ---------------------------------------------------------

	lb := r.loadBalancers[route.Path]

	backend, ok := lb.Next()

	if !ok {
		http.Error(
			w,
			"no healthy backends available",
			http.StatusServiceUnavailable,
		)
		return
	}

	log.Printf(
		"routing %s %s → %s",
		req.Method,
		req.URL.Path,
		backend.URL,
	)

	// ---------------------------------------------------------
	// 4. Parse backend target
	// ---------------------------------------------------------

	target, err := url.Parse(backend.URL)

	if err != nil {
		http.Error(
			w,
			"invalid target",
			http.StatusInternalServerError,
		)
		return
	}

	// ---------------------------------------------------------
	// 5. Create reverse proxy
	//
	// httputil.ReverseProxy supports WebSocket upgrades.
	//
	// We do NOT need to manually handle:
	//   Connection: Upgrade
	//   Upgrade: websocket
	//
	// The reverse proxy handles the upgraded connection.
	// ---------------------------------------------------------

	proxy := httputil.NewSingleHostReverseProxy(target)

	// ---------------------------------------------------------
	// 6. Handle backend failures
	// ---------------------------------------------------------

	proxy.ErrorHandler = func(
		w http.ResponseWriter,
		req *http.Request,
		err error,
	) {
		log.Printf(
			"backend failure %s %s → %s: %v",
			req.Method,
			req.URL.Path,
			backend.URL,
			err,
		)

		lb.MarkUnhealthy(backend)

		// For a normal HTTP request this becomes 502.
		//
		// For a WebSocket connection, an error occurring after
		// the connection has been upgraded means the connection
		// is already broken. We cannot transparently move that
		// existing WebSocket to another backend.
		http.Error(
			w,
			"backend unavailable",
			http.StatusBadGateway,
		)
	}

	// ---------------------------------------------------------
	// 7. Forward request
	//
	// This handles both:
	//
	// HTTP:
	//   GET /api/boards
	//
	// WebSocket:
	//   GET /api/ws
	//   Connection: Upgrade
	//   Upgrade: websocket
	//
	// Once the WebSocket is upgraded, the selected backend
	// remains attached to that connection.
	// ---------------------------------------------------------

	proxy.ServeHTTP(w, req)
}