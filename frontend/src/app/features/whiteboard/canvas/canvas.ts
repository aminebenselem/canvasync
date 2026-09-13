import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  signal,
  ViewChild
} from '@angular/core';

import { Toolbar, ToolType } from '../toolbar/toolbar';
import { Viewport, Stroke, Point, CanvasState, Shape } from './models';
import { CanvasRenderer } from '../canvas-renderer';


@Component({
  selector: 'app-canvas',
  imports: [Toolbar],
  templateUrl: './canvas.html',
  styleUrl: './canvas.css',
})
export class Canvas implements AfterViewInit {

  constructor(private canvasRenderer: CanvasRenderer) { }

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

    if (this.isShapeTool(this.activeTool) && this.currentShape) {
      this.finishShape();

      return;
    }

    if (this.isDrawing) {
      this.finishStroke();
    }
  }

  onDoubleClick(event: MouseEvent) {
    if (this.activeTool === 'select' || this.activeTool === 'text') {
      this.activeTool = 'text';
      this.updateCursor();
      this.startCaretBlink();
    } else {
      return;
    }

    const point = this.screenToWorld(event);

    const newShape: Shape = {
      type: 'text',
      start: point,
      end: point,
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
      type: this.activeTool as 'rectangle' | 'ellipse' | 'line' | 'arrow' | 'text',
      start: point,
      end: point,
      color: this.strokeColor,
      width: this.strokeWidth
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
      this.currentShape.end = point;
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

      this.currentCanvas.shapes.push(
        this.currentShape
      );

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

  onZoomChange(value: number) {
    this.viewport.zoom = value / 100;

    this.redraw();
  }

  onClear() {
    if (
      this.currentCanvas.strokes.length === 0 &&
      this.currentCanvas.shapes.length === 0
    ) {
      return;
    }

    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    ctx.clearRect(
      0,
      0,
      this.canvas.nativeElement.width,
      this.canvas.nativeElement.height
    );

    this.currentCanvas.undoStack.push(
      this.createSnapshot()
    );

    this.currentCanvas.redoStack = [];
    this.currentCanvas.strokes = [];
    this.currentCanvas.shapes = [];
  }

  onUndo() {
   
    if (this.isEditingText) {
      if(this.currentShape && this.currentShape.text) {
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

    if (this.currentShape) {
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
    if (shape.type !== 'text') {
      return false;
    }

    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return false;
    }

    ctx.font = `${shape.width}px Excalifont, "Comic Neue", cursive`;

    const width = ctx.measureText(shape.text ?? '').width;
    const height = shape.width;

    return (
      point.x >= shape.start.x &&
      point.x <= shape.start.x + width &&
      point.y >= shape.start.y &&
      point.y <= shape.start.y + height
    );
  }
}