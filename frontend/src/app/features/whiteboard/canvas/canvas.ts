import { AfterViewInit, Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { Toolbar } from '../toolbar/toolbar';


interface Point {
  x: number;
  y: number;
}
interface Stroke {
  points: Point[];
  color: string;
  width: number;
}
interface Shape {
  type: 'rectangle' | 'ellipse' | 'line' | 'arrow' | 'text' | 'sticky';
  start: Point;
  end: Point;
  color: string;
  width: number;
  text?: string;
}
interface CanvasState {
  strokes: Stroke[];
  shapes: Shape[];
  undoStack: CanvasState[];
  redoStack: CanvasState[];
}
@Component({
  selector: 'app-canvas',
  imports: [Toolbar],
  templateUrl: './canvas.html',
  styleUrl: './canvas.css',
})

export class Canvas implements AfterViewInit {
  currentCanvas: CanvasState = {
    strokes: [],
    shapes: [],
    undoStack: [],
    redoStack: []
  };
  //temporary
  penSelected = true;
  //this is preventing canvas from drawing before mouse down
  isDrawing = false;
  currentStroke: Stroke | null = null;
  @ViewChild('canvas', { static: true })
  canvas!: ElementRef<HTMLCanvasElement>;

  @HostListener('window:resize')
  onResize() {
    const canvas = this.canvas.nativeElement;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }



  ngAfterViewInit() {
    const canvas = this.canvas.nativeElement;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }

  ngOnInit() {

  }


  //need to chain mouse down , move and up events to draw on canvas
  startDrawing(event: MouseEvent) {
    if (!this.penSelected) {
      return;
    }
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }
    this.isDrawing = true;
    ctx.beginPath();
    ctx.moveTo(event.offsetX, event.offsetY);
    this.currentStroke = {
      points: [{ x: event.offsetX, y: event.offsetY }],
      color: '#000000', // default color
      width: 1, // default width
    };
  }


  keepDrawing(event: MouseEvent) {
    if (!this.penSelected || !this.isDrawing) {
      return;
    }
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    ctx.lineTo(event.offsetX, event.offsetY);
    ctx.stroke();

    if (this.currentStroke) {
      this.currentStroke.points.push({ x: event.offsetX, y: event.offsetY });
    }
  }


  finishDrawing() {
    this.isDrawing = false;
    if (!this.penSelected) {
      return;
    }
    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }
    ctx.closePath();

    if (this.currentStroke) {
      this.currentCanvas.undoStack.push(this.createSnapshot());
      this.currentCanvas.strokes.push(this.currentStroke);
      this.currentCanvas.redoStack = [];
      this.currentStroke = null;
    }
  }

  onZoomChange($event: number) {
    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }
    ctx.scale($event / 100, $event / 100);
    this.redraw();
  }


  onClear() {
    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }
    ctx.clearRect(0, 0, this.canvas.nativeElement.width, this.canvas.nativeElement.height);
    this.currentCanvas.undoStack.push({ ...this.currentCanvas });
    this.currentCanvas.redoStack = [];
    this.currentCanvas.strokes = [];
    this.currentCanvas.shapes = [];
  }


  onRedo() {
    const nextState = this.currentCanvas.redoStack.pop();

    if (!nextState) {
      return;
    }

    // Save current state so we can undo the redo
    this.currentCanvas.undoStack.push(this.createSnapshot());

    // Restore only the drawing data
    this.currentCanvas.strokes = nextState.strokes;
    this.currentCanvas.shapes = nextState.shapes;

    this.redraw();
  }


  onUndo() {
    const previousState = this.currentCanvas.undoStack.pop();

    if (!previousState) {
      return;
    }

    // Save current state so we can redo it
    this.currentCanvas.redoStack.push(this.createSnapshot());

    // Restore only the drawing data
    this.currentCanvas.strokes = previousState.strokes;
    this.currentCanvas.shapes = previousState.shapes;

    this.redraw();
  }


  onStrokeWidthChange($event: number) {
    throw new Error('Method not implemented.');
  }


  onColorChange($event: string) {
    throw new Error('Method not implemented.');
  }


  onToolChange($event: string) {
    throw new Error('Method not implemented.');
  }


  redraw() {
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

    for (const stroke of this.currentCanvas.strokes) {
      ctx.beginPath();

      ctx.moveTo(
        stroke.points[0].x,
        stroke.points[0].y
      );

      for (let i = 1; i < stroke.points.length; i++) {
        ctx.lineTo(
          stroke.points[i].x,
          stroke.points[i].y
        );
      }

      ctx.stroke();
    }
  }


  // Create an independent snapshot of the drawing state for undo/redo history.
  // The arrays are copied so future changes to the current state don't modify the snapshot.
  private createSnapshot(): CanvasState {
    return {
      strokes: [...this.currentCanvas.strokes],
      shapes: [...this.currentCanvas.shapes],
      undoStack: [],
      redoStack: []
    };
  }
}