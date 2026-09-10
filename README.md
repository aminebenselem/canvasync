# Canvasync

A real-time collaborative whiteboard built with **Java, Spring Boot, WebSockets, and Angular**.

Canvasync allows multiple clients to connect to the same whiteboard and see drawing changes in real time.

## Features

* Real-time drawing synchronization
* Multiple connected clients
* WebSocket-based communication
* HTML Canvas rendering
* Backend responsible for distributing drawing events

## Tech Stack

* **Backend:** Java + Spring Boot
* **Communication:** WebSockets
* **Frontend:** HTML, CSS, JavaScript
* **Rendering:** HTML Canvas

## Architecture

```text
Client A ──┐
           │
Client B ──┼── WebSocket ──> Spring Boot Server
           │                       │
Client C ──┘                       │
                                   └── Broadcast drawing events
```

Each client sends drawing events to the server. The server distributes those events to the other connected clients, keeping the whiteboard synchronized.

## Running Locally

### Backend

```bash
./mvnw spring-boot:run
```

### Frontend

Open the frontend in your browser or run it using the project's configured development server.

## Project Goals

This project is being built to understand how real-time systems work in practice, including:

* WebSocket communication
* Event-based architecture
* Concurrent connections
* Client/server synchronization
* Handling multiple users modifying shared state

## Status

🚧 **In development**

More functionality and synchronization logic will be added as the project evolves.
