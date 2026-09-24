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
import { Bounds, CanvasRenderer, ResizeHandle } from '../../../services/canvas-renderer';
import { WhiteboardApi } from '../../../services/whiteboard-api';
import { ActivatedRoute } from '@angular/router';
import { CreateShapeDto, CreateStrokeDto } from '../../../services/dto/dto';
import { Element } from './models';
import { CollaborationService } from '../../../services/collaboration-service';

/** Geometry of the selected element at gesture start; never mutated. */
interface Geometry {
  start?: Point;
  end?: Point;
  /** Font size for text shapes (stored in `width`). */
  width?: number;
  points?: Point[];
}

interface Gesture {
  kind: 'move' | 'resize';
  handle: ResizeHandle | null;
  startPoint: Point;
  origin: Geometry;
  bounds: Bounds;
  snapshot: CanvasState;
  moved: boolean;
}

@Component({
  selector: 'app-canvas',
  imports: [Toolbar],
  templateUrl: './canvas.html',
  styleUrl: './canvas.css',
})

export class Canvas implements AfterViewInit, OnInit {

  constructor(private canvasRenderer: CanvasRenderer,
    private whiteboardApi: WhiteboardApi,
    private route: ActivatedRoute,
    private collaborationService: CollaborationService
  ) { }

  isOwner: boolean = false;
  canEdit: boolean = false;
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

  // Selection / transform state
  private selectedElement: Shape | Stroke | null = null;
  private gesture: Gesture | null = null;

  // Text-editing state
  private editingIsNew = false;
  private editStateBefore = '';
  private editSnapshot: CanvasState | null = null;

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
      this.onSelectionKeyDown(event);
      return;
    }

    if (event.key === 'Escape') {
      this.exitTextEditing();
      return;
    }

    if (event.key === 'Enter') {
      return;
    }

    // Let shortcuts (Ctrl+Z etc.) through instead of typing the letter.
    if (event.ctrlKey || event.metaKey || event.altKey) {
      return;
    }

    if (event.key === 'Backspace') {
      this.currentShape.text =
        (this.currentShape.text ?? '').slice(0, -1);

      this.redraw();
      return;
    }

    if (event.key.length === 1) {
      this.currentShape.text = (this.currentShape.text ?? '') + event.key;
      this.redraw();
    }
  }

  private onSelectionKeyDown(event: KeyboardEvent) {
    const element = this.selectedElement;

    if (!element) {
      return;
    }

    if (event.key === 'Escape') {
      this.clearSelection();
      this.redraw();
      return;
    }

    if (
      event.key === 'Enter' &&
      this.isShape(element) &&
      (element.type === 'text' || element.type === 'sticky')
    ) {
      event.preventDefault();
      this.beginTextEditing(element, false);
    }
  }

  ngOnInit() {
    this.boardId = this.route.snapshot.paramMap.get('boardId')!;
    this.loadBoardElements();
    console.log(this.currentCanvas);
    this.isBoardOwner(this.boardId)
    this.caneEditBoard(this.boardId)
    this.collaborationService.connectToBoard(this.boardId);
  }
sendTest(): void {
  this.collaborationService.sendTest();
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
    if (!this.isOwner &&  this.activeTool !== 'pan' && !this.canEdit)  {
      return;
    }
    if (this.isEditingText && this.currentShape) {
      const point = this.screenToWorld(event);

      // Click inside the text being edited: keep editing.
      if (this.isPointInsideShape(point, this.currentShape)) {
        return;
      }

      // Click elsewhere: commit, then let the click act as a normal
      // 'select' click (exitTextEditing switches the tool back to select).
      this.exitTextEditing();
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
    if (this.activeTool === 'select') {
      this.handleElementSelection(event);
    }
  }

  onMouseMove(event: MouseEvent) {

    if (this.isPanning) {
      this.pan(event);
      return;
    }
    if (this.gesture) {
      this.updateGesture(event);
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
      return;
    }

    if (this.activeTool === 'select') {
      this.updateHoverCursor(event);
    }
  }

  onMouseUp(event: MouseEvent) {
    if (this.isPanning) {
      this.finishPanning();
      return;
    }
    if (this.gesture) {
      this.finishGesture();
      return;
    }
    if (this.isErasing) {
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
    if (!this.isOwner) {
      return;
    }
    if (this.activeTool !== 'select' && this.activeTool !== 'text') {
      return;
    }

    if (this.isEditingText) {
      return;
    }

    const point = this.screenToWorld(event);

    // Double-click on an existing text/sticky: edit it.
    const target = this.findEditableShapeAt(point);

    if (target) {
      this.selectElement(target);
      this.beginTextEditing(target, false);
      return;
    }

    // Otherwise: create a new text shape.
    const newShape: Shape = {
      id: '',
      type: 'text',
      startPoint: point,
      endPoint: point,
      color: this.strokeColor,
      width: this.textFontSize,
      text: '',
      isSelected: false
    };

    this.currentCanvas.undoStack.push(
      this.createSnapshot()
    );

    this.currentCanvas.shapes.push(newShape);
    this.currentCanvas.redoStack = [];

    this.clearSelection();
    this.beginTextEditing(newShape, true);
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
      id: '',
      points: [point],
      color: this.strokeColor,
      width: this.strokeWidth,
      isSelected: false
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

      this.createStroke(this.currentStroke);

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
      id: '',
      type: this.activeTool as 'rectangle' | 'ellipse' | 'line' | 'arrow' | 'text' | 'sticky',
      startPoint: point,
      endPoint: point,
      color: this.strokeColor,
      width: this.strokeWidth,
      text: '',
      isSelected: false
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
  // Selection, move, resize
  // -------------------------

  handleElementSelection(event: MouseEvent): void {
    if (event.button !== 0) {
      return;
    }

    const point = this.screenToWorld(event);

    // 1. Handle of the current selection -> resize
    const handle = this.hitTestHandle(point);

    if (handle) {
      this.beginGesture('resize', point, handle);
      return;
    }

    // 2. Any element under the cursor -> select + move
    const hit = this.hitTestElement(point);

    if (!hit) {
      this.clearSelection();
      this.redraw();
      return;
    }

    if (hit !== this.selectedElement) {
      this.selectElement(hit);
    }

    this.beginGesture('move', point);
    this.redraw();
  }

  private selectElement(element: Shape | Stroke): void {
    this.clearSelection();
    element.isSelected = true;
    this.selectedElement = element;
  }

  private clearSelection(): void {
    if (this.selectedElement) {
      this.selectedElement.isSelected = false;
    }

    this.selectedElement = null;
  }

  private hitTestElement(point: Point): Shape | Stroke | null {
    // Shapes render above strokes, so test them first (topmost first).
    for (let i = this.currentCanvas.shapes.length - 1; i >= 0; i--) {
      const shape = this.currentCanvas.shapes[i];

      if (this.isPointInsideShape(point, shape)) {
        return shape;
      }
    }

    for (let i = this.currentCanvas.strokes.length - 1; i >= 0; i--) {
      const stroke = this.currentCanvas.strokes[i];

      if (this.isPointNearStroke(point, stroke)) {
        return stroke;
      }
    }

    return null;
  }

  private findEditableShapeAt(point: Point): Shape | null {
    for (let i = this.currentCanvas.shapes.length - 1; i >= 0; i--) {
      const shape = this.currentCanvas.shapes[i];

      if (
        (shape.type === 'text' || shape.type === 'sticky') &&
        this.isPointInsideShape(point, shape)
      ) {
        return shape;
      }
    }

    return null;
  }

  private hitTestHandle(point: Point): ResizeHandle | null {
    const element = this.selectedElement;
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!element || !ctx) {
      return null;
    }

    const box = this.canvasRenderer.getSelectionBox(ctx, element);
    const handles = this.canvasRenderer.getHandlePositions(box);

    // 8 screen pixels regardless of zoom
    const tolerance = 8 / this.viewport.zoom;

    for (const [name, position] of Object.entries(handles) as [ResizeHandle, Point][]) {
      if (
        Math.abs(point.x - position.x) <= tolerance &&
        Math.abs(point.y - position.y) <= tolerance
      ) {
        return name;
      }
    }

    return null;
  }

  private beginGesture(
    kind: 'move' | 'resize',
    point: Point,
    handle: ResizeHandle | null = null
  ): void {
    const element = this.selectedElement;
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!element || !ctx) {
      return;
    }

    this.gesture = {
      kind,
      handle,
      startPoint: point,
      origin: this.captureGeometry(element),
      bounds: this.canvasRenderer.getElementBounds(ctx, element),
      snapshot: this.createSnapshot(),
      moved: false
    };
  }

  private updateGesture(event: MouseEvent): void {
    const gesture = this.gesture;

    if (!gesture) {
      return;
    }

    const point = this.screenToWorld(event);

    if (gesture.kind === 'move') {
      this.moveSelected(point);
    } else {
      this.resizeSelected(point);
    }
  }

  private finishGesture(): void {
    const gesture = this.gesture;
    this.gesture = null;

    if (!gesture || !gesture.moved || !this.selectedElement) {
      return;
    }

    this.currentCanvas.undoStack.push(gesture.snapshot);
    this.currentCanvas.redoStack = [];

    this.persistElement(this.selectedElement);
  }

  private moveSelected(point: Point): void {
    const gesture = this.gesture;
    const element = this.selectedElement;

    if (!gesture || !element) {
      return;
    }

    const dx = point.x - gesture.startPoint.x;
    const dy = point.y - gesture.startPoint.y;

    // Ignore sub-3px jitter so a plain click doesn't count as a move.
    if (!gesture.moved && Math.hypot(dx, dy) * this.viewport.zoom < 3) {
      return;
    }

    gesture.moved = true;

    const origin = gesture.origin;

    if (this.isShape(element)) {
      element.startPoint = { x: origin.start!.x + dx, y: origin.start!.y + dy };
      element.endPoint = { x: origin.end!.x + dx, y: origin.end!.y + dy };
    } else {
      element.points = origin.points!.map(p => ({ x: p.x + dx, y: p.y + dy }));
    }

    this.redraw();
  }

  private resizeSelected(point: Point): void {
    const gesture = this.gesture;
    const element = this.selectedElement;

    if (!gesture || !gesture.handle || !element) {
      return;
    }

    gesture.moved = true;

    const b = gesture.bounds;
    const h = gesture.handle;
    const pad = CanvasRenderer.SELECTION_PADDING;

    // Handles sit on the padded box; convert the pointer back to tight bounds.
    // A zero-extent axis (e.g. a vertical line's width) can't be scaled, so it's locked.
    const lockX = b.width === 0;
    const lockY = b.height === 0;

    let left = b.x;
    let right = b.x + b.width;
    let top = b.y;
    let bottom = b.y + b.height;

    if (!lockX && h.includes('w')) left = point.x + pad;
    if (!lockX && h.includes('e')) right = point.x - pad;
    if (!lockY && h.includes('n')) top = point.y + pad;
    if (!lockY && h.includes('s')) bottom = point.y - pad;

    // Negative scale = the element has been dragged past the opposite edge (mirror).
    const sx = lockX ? 1 : (right - left) / b.width;
    const sy = lockY ? 1 : (bottom - top) / b.height;

    const mapX = (x: number) => left + (x - b.x) * sx;
    const mapY = (y: number) => top + (y - b.y) * sy;

    if (this.isShape(element)) {
      if (element.type === 'text') {
        this.resizeText(element, gesture, h, sx, sy);
      } else {
        const origin = gesture.origin;

        element.startPoint = { x: mapX(origin.start!.x), y: mapY(origin.start!.y) };
        element.endPoint = { x: mapX(origin.end!.x), y: mapY(origin.end!.y) };
      }
    } else {
      element.points = gesture.origin.points!.map(p => ({ x: mapX(p.x), y: mapY(p.y) }));
    }

    this.redraw();
  }

  /**
   * Text has no free width/height: resizing scales the font size uniformly,
   * anchored to the corner/edge opposite the dragged handle.
   */
  private resizeText(
    shape: Shape,
    gesture: Gesture,
    handle: ResizeHandle,
    sx: number,
    sy: number
  ): void {
    const b = gesture.bounds;
    const originalSize = gesture.origin.width!;

    const candidates: number[] = [];

    if (handle.includes('e') || handle.includes('w')) candidates.push(Math.abs(sx));
    if (handle.includes('n') || handle.includes('s')) candidates.push(Math.abs(sy));

    // Use whichever axis the pointer has moved furthest from 1.
    const scale = candidates.reduce(
      (best, c) => (Math.abs(c - 1) > Math.abs(best - 1) ? c : best),
      1
    );

    const fontSize = Math.max(8, Math.round(originalSize * scale));
    const ratio = fontSize / originalSize;

    const newWidth = b.width * ratio;
    const newHeight = b.height * ratio;

    const x = handle.includes('w') ? b.x + b.width - newWidth : b.x;
    const y = handle.includes('n') ? b.y + b.height - newHeight : b.y;

    shape.width = fontSize;
    shape.startPoint = { x, y };
    shape.endPoint = { x, y };
  }

  private updateHoverCursor(event: MouseEvent): void {
    if (this.activeTool !== 'select' || this.isEditingText) {
      return;
    }

    const point = this.screenToWorld(event);
    const canvas = this.canvas.nativeElement;

    const handle = this.hitTestHandle(point);

    if (handle) {
      canvas.style.cursor = this.cursorForHandle(handle);
      return;
    }

    canvas.style.cursor = this.hitTestElement(point) ? 'move' : 'default';
  }

  private cursorForHandle(handle: ResizeHandle): string {
    switch (handle) {
      case 'nw':
      case 'se':
        return 'nwse-resize';
      case 'ne':
      case 'sw':
        return 'nesw-resize';
      case 'n':
      case 's':
        return 'ns-resize';
      default:
        return 'ew-resize';
    }
  }

  private captureGeometry(element: Shape | Stroke): Geometry {
    if (this.isShape(element)) {
      return {
        start: { ...element.startPoint },
        end: { ...element.endPoint },
        width: element.width
      };
    }

    return { points: element.points.map(p => ({ ...p })) };
  }

  private isShape(element: Shape | Stroke): element is Shape {
    return 'startPoint' in element;
  }

  // -------------------------
  // Text editing
  // -------------------------

  private beginTextEditing(shape: Shape, isNew: boolean): void {
    this.editingIsNew = isNew;
    this.editStateBefore = `${shape.text ?? ''}|${shape.width}`;
    this.editSnapshot = isNew ? null : this.createSnapshot();

    this.currentShape = shape;
    this.isEditingText = true;

    this.activeTool = 'text';
    this.updateCursor();
    this.startCaretBlink();

    this.redraw();
  }

  /**
   * Leaves text-editing mode and commits the result:
   *  - new + empty      -> discarded (including its creation undo entry)
   *  - new + non-empty  -> created on the backend
   *  - existing + empty -> deleted
   *  - existing + changed -> updated on the backend, one undo entry
   */
  private exitTextEditing() {
    const shape = this.currentShape;

    if (!shape) {
      return;
    }

    const shapes = this.currentCanvas.shapes;
    const idx = shapes.indexOf(shape);
    let keep = true;

    if (this.editingIsNew) {
      if (!shape.text) {
        if (idx !== -1) {
          shapes.splice(idx, 1);
        }

        this.currentCanvas.undoStack.pop();
        keep = false;
      } else {
        this.createShape(shape);
      }
    } else if (!shape.text) {
      if (idx !== -1) {
        shapes.splice(idx, 1);
      }

      this.deleteShape(shape);
      this.pushEditSnapshot();
      keep = false;
    } else if (`${shape.text}|${shape.width}` !== this.editStateBefore) {
      this.pushEditSnapshot();
      this.persistElement(shape);
    }

    this.isEditingText = false;
    this.activeTool = 'select';

    if (this.caretInterval) {
      clearInterval(this.caretInterval);
      this.caretInterval = null;
    }

    this.caretVisible = false;
    this.currentShape = null;
    this.editSnapshot = null;

    if (keep) {
      this.selectElement(shape);
    }

    this.updateCursor();
    this.redraw();
  }

  private pushEditSnapshot(): void {
    if (this.editSnapshot) {
      this.currentCanvas.undoStack.push(this.editSnapshot);
      this.currentCanvas.redoStack = [];
    }
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

        if (shape === this.selectedElement) {
          this.clearSelection();
        }

        this.currentCanvas.shapes.splice(i, 1);
        this.deleteShape(shape);
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

        if (stroke === this.selectedElement) {
          this.clearSelection();
        }

        this.currentCanvas.strokes.splice(i, 1);
        this.deleteStroke(stroke);
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

    this.clearSelection();
    this.currentCanvas.strokes = [];
    this.currentCanvas.shapes = [];
    this.currentCanvas.redoStack = [];

    this.redraw();
  }
  //undo and redo functions are still state only , postponed side quest
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

    this.clearSelection();

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

    this.clearSelection();

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

    if (this.activeTool !== 'select' && !this.isEditingText) {
      this.clearSelection();
      this.redraw();
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

  /**
   * Deep copy of the element geometry. Move/resize/text edits mutate
   * elements in place, so a shallow array copy would make those snapshots
   * alias the live objects and undo would restore nothing.
   */
  private createSnapshot(): CanvasState {
    return {
      strokes: this.currentCanvas.strokes.map(s => ({
        ...s,
        isSelected: false,
        points: s.points.map(p => ({ ...p }))
      })),
      shapes: this.currentCanvas.shapes.map(s => ({
        ...s,
        isSelected: false,
        startPoint: { ...s.startPoint },
        endPoint: { ...s.endPoint }
      })),
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
  private isPointInsideRect(point: Point, shape: Shape): boolean {
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

      case 'sticky':
        return this.isPointInsideRect(point, shape);

      case 'rectangle':
        return this.isPointNearRectangle(point, shape);

      case 'ellipse':
        return this.isPointNearEllipse(point, shape);

      case 'line':
      case 'arrow':
        return this.isPointNearLine(point, shape);

      default:
        return false;
    }
  }
  private isPointNearRectangle(
    point: Point,
    shape: Shape
  ): boolean {

    const minX = Math.min(shape.startPoint.x, shape.endPoint.x);
    const maxX = Math.max(shape.startPoint.x, shape.endPoint.x);
    const minY = Math.min(shape.startPoint.y, shape.endPoint.y);
    const maxY = Math.max(shape.startPoint.y, shape.endPoint.y);

    const top = this.distanceToSegment(
      point,
      { x: minX, y: minY },
      { x: maxX, y: minY }
    );

    const bottom = this.distanceToSegment(
      point,
      { x: minX, y: maxY },
      { x: maxX, y: maxY }
    );

    const left = this.distanceToSegment(
      point,
      { x: minX, y: minY },
      { x: minX, y: maxY }
    );

    const right = this.distanceToSegment(
      point,
      { x: maxX, y: minY },
      { x: maxX, y: maxY }
    );

    const distance = Math.min(top, bottom, left, right);

    return distance <= 10 + shape.width / 2;
  }
  private isPointNearEllipse(
    point: Point,
    shape: Shape
  ): boolean {

    const centerX =
      (shape.startPoint.x + shape.endPoint.x) / 2;

    const centerY =
      (shape.startPoint.y + shape.endPoint.y) / 2;

    const radiusX =
      Math.abs(shape.endPoint.x - shape.startPoint.x) / 2;

    const radiusY =
      Math.abs(shape.endPoint.y - shape.startPoint.y) / 2;

    if (radiusX === 0 || radiusY === 0) {
      return false;
    }

    const dx = point.x - centerX;
    const dy = point.y - centerY;

    const normalizedDistance =
      Math.sqrt(
        (dx * dx) / (radiusX * radiusX) +
        (dy * dy) / (radiusY * radiusY)
      );

    const tolerance =
      (10 + shape.width / 2) /
      Math.min(radiusX, radiusY);

    return Math.abs(normalizedDistance - 1) <= tolerance;
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
          console.log('Loaded board elements:', this.currentCanvas);


          this.redraw();
        },
        error: error => {
          console.error('Failed to load board elements:', error);
        }
      });
  }

  private toShapeDto(shape: Shape): CreateShapeDto {
    return {
      type: shape.type.toUpperCase() as ElementType,
      shapeType: shape.type as ShapeType,
      color: shape.color,
      width: shape.width,
      startPoint: shape.startPoint,
      endPoint: shape.endPoint,
      text: shape.text ?? ''
    };
  }

  private toStrokeDto(stroke: Stroke): CreateStrokeDto {
    return {
      color: stroke.color,
      width: stroke.width,
      points: stroke.points
    };
  }

  createShape(shape: Shape): void {
    this.whiteboardApi
      .createShape(this.boardId, this.toShapeDto(shape))
      .subscribe({
        next: (element: any) => {
          if (this.currentCanvas.shapes.includes(shape)) {
            // Shape already lives on the canvas (text created via double-click):
            // adopt the server id instead of adding a duplicate.
            shape.id = element.id;
          } else {
            this.addElementToCanvas(element);
          }

          this.redraw();
        },
        error: error => {
          console.error('Failed to create shape:', error);
        }
      });
  }
  createStroke(stroke: Stroke): void {
    this.whiteboardApi
      .createStroke(this.boardId, this.toStrokeDto(stroke))
      .subscribe({
        next: (element: any) => {
          this.addElementToCanvas(element);
          this.redraw();
        },
        error: error => {
          console.error('Failed to create stroke:', error);
        }
      });
  }

  /** Persists the current geometry/text of an existing element. */
  private persistElement(element: Shape | Stroke): void {
    if (!element.id) {
      return;
    }

    const request$ = this.isShape(element)
      ? this.whiteboardApi.updateShape(this.boardId, element.id, this.toShapeDto(element))
      : this.whiteboardApi.updateStroke(this.boardId, element.id, this.toStrokeDto(element));

    request$.subscribe({
      next: () => { },
      error: (error: unknown) => {
        console.error('Failed to update element:', error);
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
      this.currentCanvas.strokes.push({ ...element.data, id: element.id, isSelected: false });
    } else {
      this.currentCanvas.shapes.push({ ...element.data, id: element.id, isSelected: false });
    }
  }
  private deleteShape(shape: Shape): void {
    this.whiteboardApi.deleteElement(this.boardId, shape.id)
      .subscribe({
        error: error => {
          console.error('Failed to delete shape:', error);
        }
      });
  }

  private deleteStroke(stroke: Stroke): void {
    this.whiteboardApi.deleteElement(this.boardId, stroke.id)
      .subscribe({
        error: error => {
          console.error('Failed to delete stroke:', error);
        }
      });
  }
  isBoardOwner(boardId: string) {
    this.whiteboardApi.isOwner(boardId).subscribe({
      next: (isOwner: boolean) => {
        this.isOwner = isOwner;
      },
      error: (error: unknown) => {
        console.error('Failed to check ownership:', error);
        return false;
      }
    })
  }
  caneEditBoard(boardId: string) {
    this.whiteboardApi.canEdit(boardId).subscribe({
      next: (canEdit: boolean) => {
        this.canEdit = canEdit;
      },
      error: (error: unknown) => {
        console.error('Failed to check edit permission:', error);
        return false;
      }
    })
  }

}