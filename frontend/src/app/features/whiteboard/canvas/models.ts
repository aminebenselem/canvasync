
export interface Point {
  x: number;
  y: number;
}

export interface Stroke {
  points: Point[];
  color: string;
  width: number;
}

export interface Shape {
  type: ShapeType;
  startPoint: Point;
  endPoint: Point;
  color: string;
  width: number;
  text?: string;
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