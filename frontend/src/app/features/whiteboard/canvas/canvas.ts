import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnInit,
  ViewChild
} from '@angular/core';

import { Toolbar, ToolType } from '../toolbar/toolbar';
import { Viewport, Stroke, Point, CanvasState, Shape, ElementType, ShapeType } from './models';
import { CanvasRenderer } from '../../../services/canvas-renderer';
import { WhiteboardApi } from '../../../services/whiteboard-api';
import { ActivatedRoute } from '@angular/router';
import { CreateShapeDto } from '../../../services/dto/dto';
import { Element } from './models';

@Component({
  selector: 'app-canvas',
  imports: [Toolbar],
  templateUrl: './canvas.html',
  styleUrl: './canvas.css',
})
export class Canvas implements AfterViewInit  ,OnInit{

  constructor(private canvasRenderer: CanvasRenderer,
    private whiteboardApi: WhiteboardApi,
    private route: ActivatedRoute
  ) { }


  boardId!: string;
  currentCanvas: CanvasState = {
    strokes: [],
    shapes: [],
    undoStack: [],
    redoStack: []
  };

  viewport: Viewport = {
    zoom: 1,
    offsetX: 0,
    offsetY: 0
  };

  activeTool: ToolType = 'select';

  isDrawing = false;
  isPanning = false;
  isEditingText = false;
  isErasing = false;

  currentStroke: Stroke | null = null;
  currentShape: Shape | null = null;
  lastPanPoint: Point | null = null;
  currentText: string = '';

  strokeColor = '#000000';
  strokeWidth = 1;
  textFontSize = 20;
  private caretInterval: ReturnType<typeof setInterval> | null = null;
  caretVisible = true;
  @ViewChild('canvas', { static: true })
  canvas!: ElementRef<HTMLCanvasElement>;

  @ViewChild('textInput')
  textInput!: ElementRef<HTMLInputElement>;

  // -------------------------
  // Canvas lifecycle
  // -------------------------

  @HostListener('window:resize')
  onResize() {
    const canvas = this.canvas.nativeElement;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    this.redraw();
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {

    if (!this.isEditingText || !this.currentShape) {
      return;
    }

    if (event.key === 'Enter') {
      return;
    }

    if (event.key === 'Escape') {
      return;
    }

    if (event.key === 'Backspace') {
      this.currentShape.text =
        this.currentShape.text?.slice(0, -1);

      this.redraw();
      return;
    }

    if (event.key.length === 1) {
      this.currentShape.text += event.key;
      this.redraw();
    }
  }

ngOnInit() {
  this.boardId = this.route.snapshot.paramMap.get('boardId')!;
  this.loadBoardElements();
  console.log(this.currentCanvas);
}

  ngAfterViewInit() {
    

    const canvas = this.canvas.nativeElement;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    this.redraw();
  }

  // -------------------------
  // Mouse events
  // -------------------------

  onMouseDown(event: MouseEvent) {

    if (this.isEditingText && this.currentShape) {
      const point = this.screenToWorld(event);

      if (!this.isPointInsideText(point, this.currentShape)) {
        this.exitTextEditing();
        return;
      }
    }
    if (this.activeTool === 'eraser') {
      this.isErasing = true;
      this.erase(event);
      return;
    }
    if (this.activeTool === 'pan') {
      this.startPanning(event);
      return;
    }

    if (this.isShapeTool(this.activeTool)) {
      this.startShape(event);
      return;
    }

    if (this.activeTool === 'pen') {
      this.startStroke(event);
    }


  }

  onMouseMove(event: MouseEvent) {

    if (this.isPanning) {
      this.pan(event);
      return;
    }
    if (this.isErasing) {
      this.erase(event);
      return;
    }
    if (this.isShapeTool(this.activeTool) && this.currentShape) {
      this.redraw();
      this.drawShape(event);

      return;
    }

    if (this.isDrawing) {
      this.drawStroke(event);
    }
  }

  onMouseUp(event: MouseEvent) {
    if (this.isPanning) {
      this.finishPanning();
      return;
    }
    if (this.isErasing) {
      this.erase(event);
      this.isErasing = false;
      return;
    }
    if (this.isShapeTool(this.activeTool) && this.currentShape) {
      this.finishShape();

      return;
    }

    if (this.isDrawing) {
      this.finishStroke();
    }
  }

  onDoubleClick(event: MouseEvent) {
    if (this.activeTool !== 'select' && this.activeTool !== 'text') {
      return;
    }

    const point = this.screenToWorld(event);

    const shape = [...this.currentCanvas.shapes]
      .reverse()
      .find(shape =>
        shape.type === 'sticky' &&
        this.isPointInsideRectangle(point, shape)
      );

    if (shape) {
      this.currentShape = shape;
      this.isEditingText = true;
      this.activeTool = 'text';
      this.updateCursor();
      this.startCaretBlink();
      return;
    }

    // Normal text creation
    this.activeTool = 'text';
    this.updateCursor();
    this.startCaretBlink();

    const newShape: Shape = {
      type: 'text',
      startPoint: point,
      endPoint: point,
      color: this.strokeColor,
      width: this.textFontSize,
      text: ''
    };

    this.currentCanvas.undoStack.push(
      this.createSnapshot()
    );

    this.currentCanvas.shapes.push(newShape);
    this.currentCanvas.redoStack = [];

    this.currentShape = newShape;
    this.isEditingText = true;

    this.redraw();
  }




  // -------------------------
  // Drawing
  // -------------------------

  startStroke(event: MouseEvent) {
    if (event.button !== 0) {
      return;
    }

    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    const point = this.screenToWorld(event);

    this.isDrawing = true;

    ctx.beginPath();
    ctx.moveTo(point.x, point.y);

    this.currentStroke = {
      points: [point],
      color: this.strokeColor,
      width: this.strokeWidth
    };

    ctx.strokeStyle = this.currentStroke.color;
    ctx.lineWidth = this.currentStroke.width;
  }

  drawStroke(event: MouseEvent) {
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    const point = this.screenToWorld(event);

    ctx.lineTo(point.x, point.y);
    ctx.stroke();

    if (this.currentStroke) {
      this.currentStroke.points.push(point);
    }
  }

  finishStroke() {
    this.isDrawing = false;

    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    ctx.closePath();

    if (this.currentStroke) {
      this.currentCanvas.undoStack.push(
        this.createSnapshot()
      );

      this.currentCanvas.strokes.push(
        this.currentStroke
      );

      this.currentCanvas.redoStack = [];

      this.currentStroke = null;
    }
  }

  startShape(event: MouseEvent) {
    if (event.button !== 0) {
      return;
    }

    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    const point = this.screenToWorld(event);

    ctx.beginPath();
    ctx.moveTo(point.x, point.y);

    this.currentShape = {
      type: this.activeTool as 'rectangle' | 'ellipse' | 'line' | 'arrow' | 'text' | 'sticky',
      startPoint: point,
      endPoint: point,
      color: this.strokeColor,
      width: this.strokeWidth,
      text: ''
    };

    ctx.strokeStyle = this.currentShape.color;
    ctx.lineWidth = this.currentShape.width;
  }

  drawShape(event: MouseEvent) {
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    const point = this.screenToWorld(event);

    if (this.currentShape) {
      this.currentShape.endPoint = point;
    }

    this.canvasRenderer.renderShapes(
      ctx,
      this.currentCanvas.shapes.concat(
        this.currentShape ? [this.currentShape] : []
      )
    );
  }

  finishShape() {
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    ctx.closePath();

    if (this.currentShape) {
      this.currentCanvas.undoStack.push(
        this.createSnapshot()
      );

     this.createShape(this.currentShape);
      this.currentCanvas.redoStack = [];

      this.currentShape = null;
    }
  }


  // -------------------------
  // Panning
  // -------------------------

  startPanning(event: MouseEvent) {
    this.isPanning = true;

    this.canvas.nativeElement.style.cursor = 'grabbing';

    this.lastPanPoint = {
      x: event.offsetX,
      y: event.offsetY
    };
  }

  pan(event: MouseEvent) {
    if (!this.lastPanPoint) {
      return;
    }

    const dx = event.offsetX - this.lastPanPoint.x;
    const dy = event.offsetY - this.lastPanPoint.y;

    this.viewport.offsetX += dx;
    this.viewport.offsetY += dy;

    this.lastPanPoint = {
      x: event.offsetX,
      y: event.offsetY
    };

    this.redraw();
  }

  finishPanning() {
    this.isPanning = false;

    this.canvas.nativeElement.style.cursor = 'grab';

    this.lastPanPoint = null;
  }

  // -------------------------
  // Toolbar actions
  // -------------------------
  erase(event: MouseEvent) {
    const point = this.screenToWorld(event);

    // Erase shapes
    for (let i = this.currentCanvas.shapes.length - 1; i >= 0; i--) {
      const shape = this.currentCanvas.shapes[i];

      if (this.isPointInsideShape(point, shape)) {
        this.currentCanvas.undoStack.push(
          this.createSnapshot()
        );

        this.currentCanvas.shapes.splice(i, 1);
        this.currentCanvas.redoStack = [];

        this.redraw();
        return;
      }
    }

    // Erase strokes
    for (let i = this.currentCanvas.strokes.length - 1; i >= 0; i--) {
      const stroke = this.currentCanvas.strokes[i];

      if (this.isPointNearStroke(point, stroke)) {
        this.currentCanvas.undoStack.push(this.createSnapshot());

        this.currentCanvas.strokes.splice(i, 1);
        this.currentCanvas.redoStack = [];

        this.redraw();
        return;
      }
    }
  }
  onZoomChange(value: number) {
    this.viewport.zoom = value / 100;

    this.redraw();
  }

  onClear() {
   this.deleteAllElements();
    this.currentCanvas.undoStack.push(
      this.createSnapshot()
    );

    this.currentCanvas.strokes = [];
    this.currentCanvas.shapes = [];
    this.currentCanvas.redoStack = [];

    this.redraw();
  }

  onUndo() {

    if (this.isEditingText) {
      if (this.currentShape && this.currentShape.text) {
        //  this.currentShape.text = this.currentShape.text.slice(0, -1);
        this.currentShape.text = ""
        this.redraw();
      }
      return;
    }
    const previousState =
      this.currentCanvas.undoStack.pop();

    if (!previousState) {
      return;
    }

    this.currentCanvas.redoStack.push(
      this.createSnapshot()
    );

    this.currentCanvas.strokes =
      previousState.strokes;

    this.currentCanvas.shapes =
      previousState.shapes;

    this.redraw();
  }

  onRedo() {
    const nextState =
      this.currentCanvas.redoStack.pop();

    if (!nextState) {
      return;
    }

    this.currentCanvas.undoStack.push(
      this.createSnapshot()
    );

    this.currentCanvas.strokes =
      nextState.strokes;

    this.currentCanvas.shapes =
      nextState.shapes;

    this.redraw();
  }

  onStrokeWidthChange(value: number) {
    this.strokeWidth = value;
  }

  onFontSizeChange(value: number) {
    this.textFontSize = value;

    if (this.isEditingText && this.currentShape) {
      this.currentShape.width = value;
      this.redraw();
    }
  }

  onColorChange(value: string) {
    this.strokeColor = value;
  }

  onToolChange(value: string) {
    if (
      [
        'select',
        'pen',
        'eraser',
        'rectangle',
        'ellipse',
        'line',
        'arrow',
        'text',
        'sticky',
        'pan'
      ].includes(value)
    ) {
      this.activeTool = value as ToolType;
    }

    this.updateCursor();
  }

  // -------------------------
  // Rendering
  // -------------------------

  redraw() {
    const canvas = this.canvas.nativeElement;
    const ctx = canvas.getContext('2d');

    if (!ctx) {
      return;
    }

    this.canvasRenderer.redraw(
      ctx,
      canvas,
      this.currentCanvas.strokes,
      this.currentCanvas.shapes,
      this.viewport
    );

    if (
      this.currentShape &&
      (this.currentShape.type === 'text' ||
        this.currentShape.type === 'sticky') &&
      this.isEditingText
    ) {
      this.canvasRenderer.renderCaret(
        ctx,
        this.currentShape,
        this.caretVisible
      );
    }
  }

  // -------------------------
  // Helpers
  // -------------------------

  private isShapeTool(tool: ToolType): boolean {
    return [
      'rectangle',
      'ellipse',
      'line',
      'arrow',
      'sticky'
    ].includes(tool);
  }

  private createSnapshot(): CanvasState {
    return {
      strokes: [...this.currentCanvas.strokes],
      shapes: [...this.currentCanvas.shapes],
      undoStack: [],
      redoStack: []
    };
  }

  private screenToWorld(event: MouseEvent): Point {
    return {
      x:
        (event.offsetX - this.viewport.offsetX)
        / this.viewport.zoom,

      y:
        (event.offsetY - this.viewport.offsetY)
        / this.viewport.zoom
    };
  }

  private updateCursor() {
    const canvas = this.canvas.nativeElement;

    canvas.classList.remove('cursor-pen');
    canvas.style.cursor = '';

    switch (this.activeTool) {
      case 'pen':
        canvas.classList.add('cursor-pen');
        break;

      case 'pan':
        canvas.style.cursor = 'grab';
        break;

      case 'eraser':
        canvas.style.cursor = 'cell';
        break;

      case 'text':
        canvas.style.cursor = 'text';
        break;

      case 'rectangle':
      case 'ellipse':
      case 'line':
      case 'arrow':
      case 'sticky':
        canvas.style.cursor = 'crosshair';
        break;

      default:
        canvas.style.cursor = 'default';
    }
  }
  private startCaretBlink() {
    if (this.caretInterval) {
      clearInterval(this.caretInterval);
    }

    this.caretVisible = true;

    this.caretInterval = setInterval(() => {
      this.caretVisible = !this.caretVisible;
      this.redraw();
    }, 500);
  }

  /**
   * Leaves text-editing mode. If the shape being edited was left empty
   * (never typed into, or emptied via backspace), it's discarded instead
   * of being kept as a stray empty shape — including the undo snapshot
   * that was pushed for its creation, so undo history isn't left with a
   * no-op entry.
   */
  private exitTextEditing() {
    if (this.currentShape && !this.currentShape.text) {
      const idx = this.currentCanvas.shapes.indexOf(this.currentShape);

      if (idx !== -1) {
        this.currentCanvas.shapes.splice(idx, 1);
      }

      this.currentCanvas.undoStack.pop();
    }
    this.createShape(this.currentShape!);
    this.isEditingText = false;
    this.activeTool = 'select';

    if (this.caretInterval) {
      clearInterval(this.caretInterval);
      this.caretInterval = null;
    }

    this.caretVisible = false;
    this.currentShape = null;

    this.updateCursor();
    this.redraw();
  }

  private isPointInsideText(point: Point, shape: Shape): boolean {

    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return false;
    }

    ctx.font = `${shape.width}px Excalifont, "Comic Neue", cursive`;

    const width = ctx.measureText(shape.text ?? '').width;
    const height = shape.width;

    return (
      point.x >= shape.startPoint.x &&
      point.x <= shape.startPoint.x + width &&
      point.y >= shape.startPoint.y &&
      point.y <= shape.startPoint.y + height
    );
  }
  private isPointNearStroke(
    point: Point,
    stroke: Stroke
  ): boolean {
    const eraserRadius = 10;

    for (let i = 0; i < stroke.points.length - 1; i++) {
      const a = stroke.points[i];
      const b = stroke.points[i + 1];

      const distance = this.distanceToSegment(
        point,
        a,
        b
      );

      if (distance <= eraserRadius + stroke.width / 2) {
        return true;
      }
    }

    return false;
  }
  private distanceToSegment(
    p: Point,
    a: Point,
    b: Point
  ): number {
    const dx = b.x - a.x;
    const dy = b.y - a.y;

    if (dx === 0 && dy === 0) {
      return Math.hypot(
        p.x - a.x,
        p.y - a.y
      );
    }

    const t = Math.max(
      0,
      Math.min(
        1,
        ((p.x - a.x) * dx + (p.y - a.y) * dy) /
        (dx * dx + dy * dy)
      )
    );

    const closestX = a.x + t * dx;
    const closestY = a.y + t * dy;

    return Math.hypot(
      p.x - closestX,
      p.y - closestY
    );
  }
  private isPointInsideShape(point: Point, shape: Shape): boolean {
    switch (shape.type) {
      case 'text':
        return this.isPointInsideText(point, shape);

      case 'rectangle':
        return this.isPointInsideRectangle(point, shape);

      case 'ellipse':
        return this.isPointInsideEllipse(point, shape);

      case 'line':
      case 'arrow':
        return this.isPointNearLine(point, shape);

      default:
        return false;
    }
  }
  private isPointInsideRectangle(
    point: Point,
    shape: Shape
  ): boolean {
    const minX = Math.min(shape.startPoint.x, shape.endPoint.x);
    const maxX = Math.max(shape.startPoint.x, shape.endPoint.x);
    const minY = Math.min(shape.startPoint.y, shape.endPoint.y);
    const maxY = Math.max(shape.startPoint.y, shape.endPoint.y);

    return (
      point.x >= minX &&
      point.x <= maxX &&
      point.y >= minY &&
      point.y <= maxY
    );
  }
  private isPointInsideEllipse(
    point: Point,
    shape: Shape
  ): boolean {
    const centerX = (shape.startPoint.x + shape.endPoint.x) / 2;
    const centerY = (shape.startPoint.y + shape.endPoint.y) / 2;

    const radiusX = Math.abs(shape.endPoint.x - shape.startPoint.x) / 2;
    const radiusY = Math.abs(shape.endPoint.y - shape.startPoint.y) / 2;

    if (radiusX === 0 || radiusY === 0) {
      return false;
    }

    const dx = point.x - centerX;
    const dy = point.y - centerY;

    return (
      (dx * dx) / (radiusX * radiusX) +
      (dy * dy) / (radiusY * radiusY)
    ) <= 1;
  }
  private isPointNearLine(
    point: Point,
    shape: Shape
  ): boolean {
    const distance = this.distanceToSegment(
      point,
      shape.startPoint,
      shape.endPoint
    );

    return distance <= 10 + shape.width / 2;
  }




  // -------------------------
  // API calls
  // -------------------------
loadBoardElements(): void {
  this.whiteboardApi.getBoardElements(this.boardId)
    .subscribe({
      next: (elements: Element[]) => {
        console.log('Loaded board elements:', elements);
        elements.forEach(element => {
          this.addElementToCanvas(element);
        });
 
        this.redraw();
      },
      error: error => {
        console.error('Failed to load board elements:', error);
      }
    });
}

createShape(shape: Shape): void {
  const createShapeDto: CreateShapeDto = {
    type: shape.type.toUpperCase() as ElementType,
    shapeType: shape.type as ShapeType,
    color: shape.color,
    width: shape.width,
    startPoint: shape.startPoint,
    endPoint: shape.endPoint,
    text: shape.text ?? ''
  };

  this.whiteboardApi
    .createShape(this.boardId, createShapeDto)
    .subscribe({
      next: (element: any) => {
        this.addElementToCanvas(element);
        this.redraw();
      },
      error: error => {
        console.error('Failed to create shape:', error);
      }
    });
}
deleteAllElements(): void {
  this.whiteboardApi.deleteAllElements(this.boardId)
    .subscribe({
      next: () => {
        this.currentCanvas.strokes = [];
        this.currentCanvas.shapes = [];
        this.currentCanvas.undoStack = [];
        this.currentCanvas.redoStack = [];
        this.redraw();
      },
      error: error => {
        console.error('Failed to delete all elements:', error);
      }
    });
}
addElementToCanvas(element: Element): void {
if (element.type === 'STROKE') {
    this.currentCanvas.strokes.push(element.data);
} else {
    this.currentCanvas.shapes.push(element.data);
}
}
}