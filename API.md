# Whiteboard API

The Whiteboard backend exposes a REST API for persistent application operations and a STOMP-over-WebSocket API for real-time collaboration.

## Base URL

All REST API endpoints are prefixed with:

```text
/api
```

Example:

```text
GET /api/boards
```

## Authentication

Authenticated REST endpoints require a valid JWT.

The authenticated user ID is derived from the JWT `sub` claim:

```text
sub → Long userId
```

For WebSocket connections, the client authenticates using the JWT and the backend associates the WebSocket session with the authenticated user.

> `POST /login` and `POST /register` are authentication entry points and are used to obtain/create credentials rather than requiring an already-issued JWT.

---

# REST API

## Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/login` | Authenticate a user and obtain a JWT. |
| `POST` | `/register` | Register a new user. |

## User

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/me` | Retrieve the currently authenticated user. |

## Boards

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/boards` | Create a board. |
| `GET` | `/boards` | Retrieve boards available to the authenticated user. |
| `GET` | `/boards/{id}` | Retrieve a board by ID. |
| `DELETE` | `/boards/{id}` | Delete a board. |
| `POST` | `/boards/{id}/members` | Add a member to a board. |
| `GET` | `/boards/{id}/members` | Retrieve the members of a board. |
| `PATCH` | `/boards/{id}/members` | Update board member permissions. |
| `DELETE` | `/boards/{id}/members/{memberId}` | Remove a member from a board. |

## Elements

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/elements/board/{boardId}` | Retrieve all elements belonging to a board. |
| `POST` | `/elements/board/{boardId}/shape` | Create a shape element. |
| `PATCH` | `/elements/board/{boardId}/{elementId}/shape` | Update a shape element. |
| `POST` | `/elements/board/{boardId}/stroke` | Create a stroke element. |
| `PATCH` | `/elements/board/{boardId}/{elementId}/stroke` | Update a stroke element. |
| `DELETE` | `/elements/board/{boardId}/{elementId}` | Delete a single element. |
| `DELETE` | `/elements/board/{boardId}` | Delete all elements from a board. |

---

# WebSocket / STOMP API

## Connection

The WebSocket endpoint is:

```text
ws://<host>/api/ws
```

Example for local development:

```text
ws://localhost:8080/api/ws
```

The client connects using STOMP over WebSocket and sends the JWT as the STOMP `Authorization` connect header:

```text
Authorization: Bearer <JWT>
```

## Destination conventions

Client-to-server commands use `/app/...` destinations.

Server-to-client subscriptions use `/topic/...` destinations.

---

## Cursor Collaboration

### Send cursor movement

```text
/app/boards/{boardId}/cursor
```

Payload:

```json
{
  "x": 320.0,
  "y": 180.0
}
```

The backend derives the authenticated user ID and username from the WebSocket principal rather than trusting the client to provide them.

### Subscribe to cursor movements

```text
/topic/boards/{boardId}/cursor
```

Broadcast payload:

```json
{
  "userId": 42,
  "username": "username",
  "x": 320.0,
  "y": 180.0
}
```

### Subscribe to cursor departures

```text
/topic/boards/{boardId}/cursor-left
```

Payload:

```json
{
  "userId": 42
}
```

Cursor state is transient and is not persisted in PostgreSQL.

---

## Drawing Collaboration

### Send drawing events

```text
/app/boards/{boardId}/drawing
```

Drawing events support transient gesture updates such as starting, updating, and ending drawing or element-transform operations.

Example:

```json
{
  "action": "UPDATE",
  "elementType": "STROKE",
  "elementId": "2fca8f89-4e5d-4df4-b978-8d65df0e5c72",
  "operation": "DRAW",
  "point": {
    "x": 220.0,
    "y": 140.0
  },
  "color": "#111111",
  "width": 2.0
}
```

Supported event fields include:

```text
action       START | UPDATE | END
elementType  STROKE | SHAPE
elementId    optional
action data  operation / shapeType / point / startPoint / endPoint
color        optional
width        optional
points       optional list of points
```

### Subscribe to drawing events

```text
/topic/boards/{boardId}/drawing
```

The server broadcasts a board event containing the authenticated actor and the drawing event payload.

Example structure:

```json
{
  "userId": 42,
  "event": {
    "action": "UPDATE",
    "elementType": "STROKE",
    "operation": "DRAW",
    "point": {
      "x": 220.0,
      "y": 140.0
    }
  }
}
```

Drawing messages are realtime collaboration data. The final persistent element state is stored through the normal element application flow.

---

## Element Collaboration Events

Clients subscribe to:

```text
/topic/boards/{boardId}/elements
```

The server publishes element changes for operations such as:

```text
ELEMENT_CREATED
ELEMENT_UPDATED
ELEMENT_DELETED
ELEMENTS_CLEARED
```

The event envelope contains:

```json
{
  "type": "ELEMENT_UPDATED",
  "boardId": "c4f85bda-0443-4144-9f8f-cd25fb731b37",
  "payload": {
    "actorId": 42,
    "element": {},
    "elementIds": null
  }
}
```

`actorId` identifies the user that initiated the persistent change. Clients can use it to avoid replaying their own updates locally.

---

## Create Element via WebSocket

The collaboration layer also exposes a create command:

```text
/app/boards/{boardId}/elements/create
```

The message contains an element type and the corresponding payload.

Example shape command:

```json
{
  "elementType": "SHAPE",
  "shape": {}
}
```

Example stroke command:

```json
{
  "elementType": "STROKE",
  "stroke": {}
}
```

The authenticated user must have editor permission for the board.

Persistent element changes are published by the element service after the database transaction commits, so REST writes and collaboration-driven writes can produce the same realtime element events.

---

# Authorization

Board collaboration uses two permissions:

```text
EDITOR
VIEWER
```

`EDITOR` users can modify board content.

`VIEWER` users can subscribe to and observe board collaboration traffic but cannot perform editing operations.

Board ownership is handled separately by the board access policy.

For WebSocket subscriptions, the backend validates that the authenticated user can view the requested board before allowing the subscription.

For editing commands, the backend validates that the authenticated user can edit the board.

Persistent business operations also perform their own authorization checks in the application/service layer.

---

# Realtime Event Flow

A collaboration command follows this general path:

```text
Angular Client
      │
      │ STOMP SEND
      ▼
API Gateway
      │
      ▼
Spring Boot Backend Instance
      │
      │ Authenticate + Authorize
      ▼
Collaboration Controller
      │
      │ Publish board event
      ▼
Redis Pub/Sub
      │
      ├──────────────► Backend Instance A
      │
      ├──────────────► Backend Instance B
      │
      └──────────────► Backend Instance N
                              │
                              ▼
                       Connected Clients
```

Redis is used for cross-instance event propagation so clients connected to different backend instances can participate in the same board session.

---

# API Summary

```text
REST
/api/login
/api/register
/api/me
/api/boards
/api/boards/{id}
/api/boards/{id}/members
/api/boards/{id}/members/{memberId}
/api/elements/board/{boardId}
/api/elements/board/{boardId}/shape
/api/elements/board/{boardId}/{elementId}/shape
/api/elements/board/{boardId}/stroke
/api/elements/board/{boardId}/{elementId}/stroke
/api/elements/board/{boardId}/{elementId}

WebSocket
/api/ws

STOMP SEND
/app/boards/{boardId}/cursor
/app/boards/{boardId}/drawing
/app/boards/{boardId}/elements/create

STOMP SUBSCRIBE
/topic/boards/{boardId}/cursor
/topic/boards/{boardId}/cursor-left
/topic/boards/{boardId}/drawing
/topic/boards/{boardId}/elements
```

