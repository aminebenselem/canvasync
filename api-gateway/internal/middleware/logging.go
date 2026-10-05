package middleware

import (
	"bufio"
	"fmt"
	"log"
	"net"
	"net/http"
	"time"
)

type responseWriter struct {
	http.ResponseWriter
	statusCode int
}

func (rw *responseWriter) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

func (rw *responseWriter) Write(body []byte) (int, error) {
	if rw.statusCode == 0 {
		rw.statusCode = http.StatusOK
	}

	return rw.ResponseWriter.Write(body)
}

// Hijack allows WebSocket connections to take over the underlying
// TCP connection.
func (rw *responseWriter) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	hijacker, ok := rw.ResponseWriter.(http.Hijacker)

	if !ok {
		return nil, nil, fmt.Errorf(
			"underlying ResponseWriter does not support hijacking",
		)
	}

	return hijacker.Hijack()
}

// Unwrap allows http.ResponseController and other HTTP utilities
// to access the original ResponseWriter.
func (rw *responseWriter) Unwrap() http.ResponseWriter {
	return rw.ResponseWriter
}

func Logging(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

		start := time.Now()

		rw := &responseWriter{
			ResponseWriter: w,
		}

		next.ServeHTTP(rw, r)

		duration := time.Since(start)

		log.Printf(
			"%s %s → %d → %v",
			r.Method,
			r.URL.Path,
			rw.statusCode,
			duration,
		)
	})
}