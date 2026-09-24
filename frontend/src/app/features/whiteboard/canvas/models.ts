
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
