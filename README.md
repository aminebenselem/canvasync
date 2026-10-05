# Collaborative Whiteboard

A real-time collaborative whiteboard built with Angular and Spring Boot, designed around WebSocket/STOMP communication, Redis Pub/Sub, PostgreSQL persistence, and a scalable backend deployment model behind an API Gateway.

The application allows multiple users to work on the same board with role-based access control and real-time synchronization of cursors and whiteboard operations.

## Highlights

- JWT-based authentication
- Boards, memberships, and invitations
- Viewer and Editor permissions
- Persistent whiteboard elements
- Real-time cursor synchronization
- Real-time drawing synchronization
- Real-time element creation, movement, resizing, and deletion
- Redis Pub/Sub for cross-instance event propagation
- Redis-backed board permission caching for WebSocket collaboration
- Multiple Spring Boot backend instances
- Go API Gateway for routing, rate limiting, health checks, and load balancing
- PostgreSQL as the persistent data store
- Docker / Docker Compose deployment
- Unit and integration tests

## System Architecture

![Collaborative Whiteboard System Architecture](docs/images/system-architecture.png)

The system separates persistent application state from transient collaboration events:

```text
                    HTTP / REST
Angular Clients ───────────────────────► API Gateway
                                           │
                                           ▼
                                  Spring Boot Backend
                                  ┌────────┴────────┐
                                  │                 │
                             Instance A        Instance B ...
                                  │                 │
                                  └────────┬────────┘
                                           │
                              Redis Pub/Sub + Cache
                                           │
                                           ▼
                                      PostgreSQL
```

The API Gateway provides a single entry point and distributes requests across healthy backend instances. WebSocket connections are established through the gateway and remain associated with the backend instance that accepted the connection.

Redis provides two distinct responsibilities:

1. **Pub/Sub** propagates collaboration events between backend instances.
2. **Caching** stores board permissions used by the real-time authorization path.

PostgreSQL remains the source of truth for persistent application data.

## Real-Time Collaboration

The frontend communicates with the backend using STOMP over WebSockets.

### Application destinations

Clients send collaboration commands to destinations such as:

```text
/app/boards/{boardId}/cursor
/app/boards/{boardId}/drawing
/app/boards/{boardId}/elements/create
```

### Subscription destinations

Clients subscribe to board topics such as:

```text
/topic/boards/{boardId}/cursor
/topic/boards/{boardId}/cursor-left
/topic/boards/{boardId}/drawing
/topic/boards/{boardId}/elements
```

A typical realtime event follows this path:

```text
Client
  │
  │ STOMP SEND
  ▼
Backend instance
  │
  │ authenticate + authorize
  ▼
Collaboration Controller
  │
  │ publish board event
  ▼
Redis Pub/Sub
  │
  ├──────────────► Backend instance A
  │
  └──────────────► Backend instance B
                         │
                         ▼
                 Connected WebSocket clients
```

This means a user connected to one backend instance can collaborate with another user connected to a different instance.

## Persistence vs. Realtime State

Realtime communication and persistence are intentionally separated.

During a drawing gesture, transient `START` and `UPDATE` events are sent through WebSocket/STOMP. The final element state is persisted through the regular application service/database flow.

```text
Mouse Down
    │
    ▼
DRAWING START ─────► WebSocket ─────► Redis ─────► Clients
    │
    ▼
DRAWING UPDATE ────► WebSocket ─────► Redis ─────► Clients
    │
    ▼
Mouse Up
    │
    ▼
Final Element State ────────────────► Element Service ───► PostgreSQL
```

Cursor movement is transient and is not persisted as application data.

## Authorization Model

Boards support two collaborative permissions:

- `EDITOR` — can modify board content
- `VIEWER` — can observe the board but cannot perform editing operations

Board ownership is handled separately by the board access policy rather than introducing a third cached permission value.

The WebSocket path performs an early permission check for collaboration commands. Persistent business operations also enforce authorization in the service layer.

This keeps transport-level authorization and business-level authorization independent:

```text
WebSocket Authentication
        │
        ▼
Identify User
        │
        ▼
Load / cache board permission
        │
        ▼
Authorize realtime command
        │
        ▼
Collaboration Controller
        │
        ▼
Application Service
        │
        ▼
Authoritative business checks
        │
        ▼
PostgreSQL
```

## Technology Stack

### Frontend

- Angular
- TypeScript
- HTML Canvas
- STOMP.js

### Backend

- Java
- Spring Boot
- Spring Security
- Spring WebSocket
- STOMP
- Spring Data JPA
- Flyway

### Infrastructure

- Go API Gateway
- Redis
- PostgreSQL
- Docker
- Docker Compose

## Running Locally

### Prerequisites

- Java 23
- Node.js
- npm
- Docker / Docker Compose
- PostgreSQL
- Redis

### Infrastructure

Start the infrastructure services:

```bash
docker compose up -d
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```powershell
cd backend
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

## Testing

Run the backend test suite with:

```bash
./mvnw test
```

On Windows:

```powershell
mvnw.cmd test
```

The test suite includes unit and integration coverage for core services and collaboration controllers.

## Project Structure

```text
whiteboard/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   └── package.json
│
├── docs/
│   └── images/
│       └── system-architecture.png
│
└── docker-compose.yml
```

## Documentation

More detailed architectural decisions and realtime flows are documented in [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Future Improvements

Potential future work includes stronger concurrent-edit conflict handling, richer presence features, production observability, and further horizontal-scaling improvements.

## API Documentation

See [API.md](API.md) for the complete REST and WebSocket/STOMP API reference.
