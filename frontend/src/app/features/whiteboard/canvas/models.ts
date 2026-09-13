
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
  type: 'rectangle' | 'ellipse' | 'line' | 'arrow' | 'text' | 'sticky';
  start: Point;
  end: Point;
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