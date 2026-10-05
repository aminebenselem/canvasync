
export interface Point {
  x: number;
  y: number;
}

export interface Stroke {
  id: string;
  points: Point[];
  color: string;
  width: number;
  isSelected: boolean;
}

export interface Shape {
  id: string;
  type: ShapeType;
  startPoint: Point;
  endPoint: Point;
  color: string;
  width: number;
  text?: string;
  isSelected: boolean;
}

export interface CanvasState {
  strokes: Stroke[];
  shapes: Shape[];
  undoStack: CanvasState[];
  redoStack: CanvasState[];
}

export interface Viewport {
  zoom: number;
  offsetX: number;
  offsetY: number;
}
export type Element =
  | {  
      id: string;
      type: 'STROKE';
      data: Stroke;
    }
  | {
      id: string;
      type: 'RECTANGLE' | 'ELLIPSE' | 'LINE' | 'ARROW' | 'TEXT';
      data: Shape;
    };

export interface Board{
  id: string;
  name: string;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}
export type ElementType =
  | 'STROKE'
  | 'RECTANGLE'
  | 'ELLIPSE'
  | 'LINE'
  | 'ARROW'
  | 'TEXT';
export type ShapeType =
  | 'rectangle'
  | 'ellipse'
  | 'line'
  | 'arrow'
  | 'text'
  | 'sticky';

  export type BoardPermission = 'VIEWER' | 'EDITOR';

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface Invitation {
  id: string;
  boardId: string;
  senderId: number;
  userId: number;
  permission: BoardPermission;
  status: InvitationStatus;
}

export interface BoardMember {
  userId: number;
  username: string;
  email: string;
  permission: BoardPermission;
}
export interface CursorMovedEvent {
  userId: number;
  username: string;
  x: number;
  y: number;
}
export interface CursorLeftEvent {
  userId: number;
}

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

export interface DrawingEvent {
  action: 'START' | 'UPDATE' | 'END';

  elementType: 'STROKE' | 'SHAPE';

  elementId?: string;

  operation?: 'DRAW' | 'MOVE' | 'RESIZE';

  shapeType?: string;

  point?: {
    x: number;
    y: number;
  };

  startPoint?: {
    x: number;
    y: number;
  };

  endPoint?: {
    x: number;
    y: number;
  };

  color?: string;

  width?: number;

  points?: {
    x: number;
    y: number;
  }[];
}
export interface DrawingBroadcast {
  userId: number;
  event: DrawingEvent;
}