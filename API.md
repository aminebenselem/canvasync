# Whiteboard API
All endpoint starts with /api
## Authentication
All endpoints require a valid JWT.

User ID is extracted from:
sub → Long userId

POST /login


POST /register
## User

GET     /me


## Boards

POST   /boards

GET    /boards

GET    /boards/{id}

DELETE /boards/{id}

POST   /boards/{id}/members

GET    /boards/{id}/members

PATCH  /boards/{id}/members

DELETE /boards/{id}/members/{memberId}

## Elements

GET    /elements/board/{boardId}

POST   /elements/board/{boardId}/shape

PATCH  /elements/board/{boardId}/{elementId}/shape

POST   /elements/board/{boardId}/stroke

PATCH  /elements/board/{boardId}/{elementId}/stroke

DELETE /elements/board/{boardId}/{elementId}

DELETE /elements/board/{boardId}