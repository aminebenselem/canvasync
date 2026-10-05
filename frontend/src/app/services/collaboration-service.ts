import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Subject } from 'rxjs';

import {
  CursorLeftEvent,
  CursorMovedEvent,
  DrawingBroadcast,
  Element,
  Point,
  Shape,
  Stroke
} from '../features/whiteboard/canvas/models';

export type ElementEventType =
  | 'ELEMENT_CREATED'
  | 'ELEMENT_UPDATED'
  | 'ELEMENT_DELETED'
  | 'ELEMENTS_CLEARED';

export interface ElementChangeEvent {
  type: ElementEventType;

  boardId: string;

  payload: {
    actorId: number;
    element: Element | null;
    elementIds: string[] | null;
  };
}

/**
 * Redis/Spring broadcasts drawing events inside this envelope.
 *
 * {
 *   type: "DRAWING",
 *   boardId: "...",
 *   payload: {
 *     userId: 2,
 *     event: {
 *       action: "UPDATE",
 *       ...
 *     }
 *   }
 * }
 */
interface BoardEventEnvelope<T> {
  type: string;
  boardId: string;
  payload: T;
}

@Injectable({
  providedIn: 'root'
})
export class CollaborationService {

  private client: Client;

  private currentBoardId?: string;

  currentUserId!: number;

  // =====================================================
  // THROTTLING
  // =====================================================

  private lastCursorSent = 0;
  private readonly cursorInterval = 33; // ~30 FPS

  private lastDrawingSent = 0;
  private readonly drawingInterval = 33; // ~30 FPS

  // =====================================================
  // CURSORS
  // =====================================================

  remoteCursors =
    new Map<number, CursorMovedEvent>();

  private cursorSubject =
    new Subject<CursorMovedEvent>();

  cursor$ =
    this.cursorSubject.asObservable();

  // =====================================================
  // ELEMENTS
  // =====================================================

  private elementsSubject =
    new Subject<ElementChangeEvent>();

  elements$ =
    this.elementsSubject.asObservable();

  // =====================================================
  // DRAWING
  // =====================================================

  private drawingSubject =
    new Subject<DrawingBroadcast>();

  drawing$ =
    this.drawingSubject.asObservable();

  constructor() {

    this.client = new Client({

      brokerURL:
        'ws://localhost:8080/api/ws',

      onConnect: () => {

        console.log('WebSocket connected');

        if (!this.currentBoardId) {
          return;
        }

        this.subscribeToCursor(
          this.currentBoardId
        );

        this.subscribeToCursorLeft(
          this.currentBoardId
        );

        this.subscribeToElements(
          this.currentBoardId
        );

        this.subscribeToDrawing(
          this.currentBoardId
        );
      },

      onDisconnect: () => {

        console.log(
          'WebSocket disconnected'
        );
      },

      onStompError: error => {

        console.error(
          'STOMP error:',
          error
        );
      }
    });
  }

  // =====================================================
  // CONNECTION
  // =====================================================

  connectToBoard(boardId: string): void {

    this.currentBoardId = boardId;

    this.currentUserId =
      parseInt(
        localStorage.getItem('userId') || '0',
        10
      );

    this.lastCursorSent = 0;
    this.lastDrawingSent = 0;

    this.client.connectHeaders = {
      Authorization:
        `Bearer ${localStorage.getItem('access_token')}`
    };

    this.client.activate();
  }

  disconnect(): void {

    this.currentBoardId = undefined;

    this.remoteCursors.clear();

    this.client.deactivate();
  }

  // =====================================================
  // CURSORS
  // =====================================================

  subscribeToCursor(
    boardId: string
  ): void {

    this.client.subscribe(

      `/topic/boards/${boardId}/cursor`,

      message => {

        const event =
          JSON.parse(
            message.body
          ) as CursorMovedEvent;

        this.handleRemoteCursor(
          event
        );
      }
    );
  }

  subscribeToCursorLeft(
    boardId: string
  ): void {

    this.client.subscribe(

      `/topic/boards/${boardId}/cursor-left`,

      message => {

        const event =
          JSON.parse(
            message.body
          ) as CursorLeftEvent;

        this.remoteCursors.delete(
          event.userId
        );

        this.cursorSubject.next({

          userId: event.userId,

          username: '',

          x: 0,

          y: 0
        });
      }
    );
  }

  sendCursorEvent(
    x: number,
    y: number
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    const now = Date.now();

    if (
      now - this.lastCursorSent
      < this.cursorInterval
    ) {
      return;
    }

    this.lastCursorSent = now;

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/cursor`,

      body: JSON.stringify({
        x,
        y
      })
    });
  }

  private handleRemoteCursor(
    event: CursorMovedEvent
  ): void {

    if (
      event.userId ===
      this.currentUserId
    ) {
      return;
    }

    this.remoteCursors.set(
      event.userId,
      event
    );

    this.cursorSubject.next(
      event
    );
  }

  getRemoteCursors():
    Map<number, CursorMovedEvent> {

    return this.remoteCursors;
  }

  // =====================================================
  // ELEMENTS
  // =====================================================

  subscribeToElements(
    boardId: string
  ): void {

    this.client.subscribe(

      `/topic/boards/${boardId}/elements`,

      message => {

        const event =
          JSON.parse(
            message.body
          ) as ElementChangeEvent;

        /*
         * This client already changed its local canvas.
         * Do not apply its own persistent event again.
         */
        if (
          event.payload?.actorId ===
          this.currentUserId
        ) {
          return;
        }

        this.elementsSubject.next(
          event
        );
      }
    );
  }

  // =====================================================
  // DRAWING SUBSCRIPTION
  // =====================================================

  subscribeToDrawing(
    boardId: string
  ): void {

    this.client.subscribe(

      `/topic/boards/${boardId}/drawing`,

      message => {

        const envelope =
          JSON.parse(
            message.body
          ) as BoardEventEnvelope<DrawingBroadcast>;

        /*
         * Backend sends:
         *
         * {
         *   type,
         *   boardId,
         *   payload: DrawingBroadcast
         * }
         *
         * We need payload.
         */
        const event =
          envelope.payload;

        if (!event) {
          return;
        }

        /*
         * Ignore our own transient drawing.
         */
        if (
          event.userId ===
          this.currentUserId
        ) {
          return;
        }

        this.drawingSubject.next(
          event
        );
      }
    );
  }

  // =====================================================
  // DRAWING SEND
  // =====================================================

  sendDrawingStart(
    point: Point,
    color: string,
    width: number
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'START',

        elementType: 'STROKE',

        point,

        color,

        width
      })
    });
  }

  sendStrokePoint(
    point: Point
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    const now = Date.now();

    if (
      now - this.lastDrawingSent
      < this.drawingInterval
    ) {
      return;
    }

    this.lastDrawingSent = now;

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'UPDATE',

        elementType: 'STROKE',

        point
      })
    });
  }

  /**
   * Sends the final point without throttling.
   *
   * This guarantees that the last mouse position
   * is not lost because it happened inside the
   * throttling window.
   */
  sendStrokePointFinal(
    point: Point
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'UPDATE',

        elementType: 'STROKE',

        point
      })
    });
  }

  sendShapeStart(
    shape: Shape
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'START',

        elementType: 'SHAPE',

        shapeType: shape.type,

        startPoint:
          shape.startPoint,

        endPoint:
          shape.endPoint,

        color:
          shape.color,

        width:
          shape.width
      })
    });
  }

  sendShapeUpdate(
    shape: Shape
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    const now = Date.now();

    if (
      now - this.lastDrawingSent
      < this.drawingInterval
    ) {
      return;
    }

    this.lastDrawingSent = now;

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'UPDATE',

        elementType: 'SHAPE',

        shapeType:
          shape.type,

        startPoint:
          shape.startPoint,

        endPoint:
          shape.endPoint,

        color:
          shape.color,

        width:
          shape.width
      })
    });
  }

  /**
   * Final shape update without throttling.
   */
  sendShapeUpdateFinal(
    shape: Shape
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'UPDATE',

        elementType: 'SHAPE',

        shapeType:
          shape.type,

        startPoint:
          shape.startPoint,

        endPoint:
          shape.endPoint,

        color:
          shape.color,

        width:
          shape.width
      })
    });
  }

  sendDrawingEnd(
    elementType:
      'STROKE' | 'SHAPE'
  ): void {

    if (
      !this.client.connected ||
      !this.currentBoardId
    ) {
      return;
    }

    this.client.publish({

      destination:
        `/app/boards/${this.currentBoardId}/drawing`,

      body: JSON.stringify({

        action: 'END',

        elementType
      })
    });
  }
  sendTransformStart(
  element: Shape | Stroke,
  operation: 'MOVE' | 'RESIZE'
): void {

  if (
    !this.client.connected ||
    !this.currentBoardId ||
    !element.id
  ) {
    return;
  }

  const isShape =
    'startPoint' in element;

  this.client.publish({

    destination:
      `/app/boards/${this.currentBoardId}/drawing`,

    body: JSON.stringify({

      action: 'START',

      elementType:
        isShape ? 'SHAPE' : 'STROKE',

      elementId:
        element.id,

      operation,

      ...(isShape
        ? {
            shapeType: element.type,
            startPoint: element.startPoint,
            endPoint: element.endPoint,
            color: element.color,
            width: element.width
          }
        : {
            points: element.points,
            color: element.color,
            width: element.width
          })
    })
  });
}
sendTransformUpdate(
  element: Shape | Stroke,
  operation: 'MOVE' | 'RESIZE'
): void {

  if (
    !this.client.connected ||
    !this.currentBoardId ||
    !element.id
  ) {
    return;
  }

  const now = Date.now();

  if (
    now - this.lastDrawingSent <
    this.drawingInterval
  ) {
    return;
  }

  this.lastDrawingSent = now;

  const isShape =
    'startPoint' in element;

  this.client.publish({

    destination:
      `/app/boards/${this.currentBoardId}/drawing`,

    body: JSON.stringify({

      action: 'UPDATE',

      elementType:
        isShape ? 'SHAPE' : 'STROKE',

      elementId:
        element.id,

      operation,

      ...(isShape
        ? {
            shapeType: element.type,
            startPoint: element.startPoint,
            endPoint: element.endPoint,
            color: element.color,
            width: element.width
          }
        : {
            points: element.points,
            color: element.color,
            width: element.width
          })
    })
  });
}
sendTransformUpdateFinal(
  element: Shape | Stroke,
  operation: 'MOVE' | 'RESIZE'
): void {

  if (
    !this.client.connected ||
    !this.currentBoardId ||
    !element.id
  ) {
    return;
  }

  const isShape =
    'startPoint' in element;

  this.client.publish({

    destination:
      `/app/boards/${this.currentBoardId}/drawing`,

    body: JSON.stringify({

      action: 'UPDATE',

      elementType:
        isShape ? 'SHAPE' : 'STROKE',

      elementId:
        element.id,

      operation,

      ...(isShape
        ? {
            shapeType: element.type,
            startPoint: element.startPoint,
            endPoint: element.endPoint,
            color: element.color,
            width: element.width
          }
        : {
            points: element.points,
            color: element.color,
            width: element.width
          })
    })
  });
}
sendTransformEnd(
  element: Shape | Stroke,
  operation: 'MOVE' | 'RESIZE'
): void {

  if (
    !this.client.connected ||
    !this.currentBoardId ||
    !element.id
  ) {
    return;
  }

  this.client.publish({

    destination:
      `/app/boards/${this.currentBoardId}/drawing`,

    body: JSON.stringify({

      action: 'END',

      elementType:
        'startPoint' in element
          ? 'SHAPE'
          : 'STROKE',

      elementId:
        element.id,

      operation
    })
  });
}
}