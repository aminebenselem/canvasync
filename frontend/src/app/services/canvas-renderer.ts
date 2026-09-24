import { Injectable } from '@angular/core';
import { Point, Shape, Stroke, Viewport } from '../features/whiteboard/canvas/models';
import { drawRectangle } from '../features/whiteboard/shapes/rectangle';
import { drawEllipse } from '../features/whiteboard/shapes/ellipse';
import { drawArrow } from '../features/whiteboard/shapes/arrow';
import { drawLine } from '../features/whiteboard/shapes/line';
import { drawStickyNote } from '../features/whiteboard/shapes/stickynote';

export interface Bounds {
  x: number;
  y: number;
  width: number;
  height: number;
}

export type ResizeHandle = 'nw' | 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w';

interface StickyLayout {
  lines: string[];
  centerX: number;
  startY: number;
  lineHeight: number;
}

@Injectable({
  providedIn: 'root'
})
export class CanvasRenderer {
  /** World-space padding between an element's bounds and its selection box. */
  static readonly SELECTION_PADDING = 6;

  private readonly stickyFontSize = 16;
  private readonly stickyPadding = 10;

  redraw(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    strokes: Stroke[],
    shapes: Shape[],
    viewport: Viewport
  ) {

    ctx.setTransform(1, 0, 0, 1, 0, 0);

    ctx.clearRect(
      0,
      0,
      canvas.width,
      canvas.height
    );

    ctx.setTransform(
      viewport.zoom,
      0,
      0,
      viewport.zoom,
      viewport.offsetX,
      viewport.offsetY
    );

    this.renderStrokes(ctx, strokes);
    this.renderShapes(ctx, shapes);
    this.renderText(ctx, shapes);

    this.renderSelections(ctx, strokes, shapes, viewport.zoom);
  }

  renderShapes(ctx: CanvasRenderingContext2D, shapes: Shape[]) {
    for (const shape of shapes) {
      ctx.strokeStyle = shape.color;
      ctx.lineWidth = shape.width;
      switch (shape.type) {
        case 'sticky':
          drawStickyNote(ctx, shape.startPoint, shape.endPoint);
          break;
        case 'line':
          drawLine(ctx, shape.startPoint, shape.endPoint);
          break;

        case 'arrow':
          ctx.lineWidth = 2;
          drawArrow(ctx, shape.startPoint, shape.endPoint);
          break;
        case 'rectangle':
          drawRectangle(ctx, shape.startPoint, shape.endPoint);
          break;
        case 'ellipse':
          drawEllipse(
            ctx,
            shape.startPoint,
            shape.endPoint
          );
          break;
      }

    }
  }

  renderStrokes(ctx: CanvasRenderingContext2D, strokes: Stroke[]) {
    for (const stroke of strokes) {

      ctx.beginPath();

      ctx.strokeStyle = stroke.color;
      ctx.lineWidth = stroke.width;

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

  renderText(ctx: CanvasRenderingContext2D, shapes: Shape[]) {
    for (const shape of shapes) {
      if (shape.type === 'text' && shape.text) {
        ctx.fillStyle = shape.color;
        ctx.font = `${shape.width}px Excalifont, "Comic Neue", cursive`;
        // textAlign is persistent context state: reset it, otherwise a text
        // drawn after a sticky inherits the sticky's 'center' alignment.
        ctx.textAlign = 'left';
        ctx.textBaseline = 'top';
        ctx.fillText(shape.text, shape.startPoint.x, shape.startPoint.y);
      }

      if (shape.type === 'sticky' && shape.text) {
        const layout = this.layoutStickyText(ctx, shape);

        ctx.fillStyle = '#3a3a3a';

        layout.lines.forEach((line, index) => {
          ctx.fillText(
            line,
            layout.centerX,
            layout.startY + index * layout.lineHeight
          );
        });
      }
    }
  }

  renderCaret(
    ctx: CanvasRenderingContext2D,
    shape: Shape,
    visible: boolean
  ) {
    if (!visible) {
      return;
    }

    let x: number;
    let y: number;
    let height: number;

    if (shape.type === 'sticky') {
      // Caret follows the same wrapped, centered layout as renderText.
      const layout = this.layoutStickyText(ctx, shape);
      const lastIndex = layout.lines.length - 1;
      const lastWidth = ctx.measureText(layout.lines[lastIndex]).width;

      x = layout.centerX + lastWidth / 2;
      y = layout.startY + lastIndex * layout.lineHeight;
      height = this.stickyFontSize;
    } else {
      ctx.font = `${shape.width}px Excalifont, "Comic Neue", cursive`;
      ctx.textAlign = 'left';
      ctx.textBaseline = 'top';

      x = shape.startPoint.x + ctx.measureText(shape.text ?? '').width;
      y = shape.startPoint.y;
      height = shape.width;
    }

    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(x, y + height);

    ctx.strokeStyle = shape.color;
    ctx.lineWidth = 1;
    ctx.stroke();
  }

  // -------------------------
  // Geometry
  // -------------------------

  /** Tight bounds of an element in world space (no selection padding). */
  getElementBounds(ctx: CanvasRenderingContext2D, element: Shape | Stroke): Bounds {
    if ('points' in element) {
      if (!element.points.length) {
        return { x: 0, y: 0, width: 0, height: 0 };
      }

      let minX = element.points[0].x;
      let minY = element.points[0].y;
      let maxX = minX;
      let maxY = minY;

      for (const point of element.points) {
        minX = Math.min(minX, point.x);
        minY = Math.min(minY, point.y);
        maxX = Math.max(maxX, point.x);
        maxY = Math.max(maxY, point.y);
      }

      return { x: minX, y: minY, width: maxX - minX, height: maxY - minY };
    }

    if (element.type === 'text') {
      ctx.save();
      ctx.font = `${element.width}px Excalifont, "Comic Neue", cursive`;
      const width = ctx.measureText(element.text ?? '').width;
      ctx.restore();

      // Text shapes store their font size in `width`; endPoint is unused.
      return {
        x: element.startPoint.x,
        y: element.startPoint.y,
        width,
        height: element.width
      };
    }

    return {
      x: Math.min(element.startPoint.x, element.endPoint.x),
      y: Math.min(element.startPoint.y, element.endPoint.y),
      width: Math.abs(element.endPoint.x - element.startPoint.x),
      height: Math.abs(element.endPoint.y - element.startPoint.y)
    };
  }

  /** Bounds inflated by SELECTION_PADDING — the box the handles sit on. */
  getSelectionBox(ctx: CanvasRenderingContext2D, element: Shape | Stroke): Bounds {
    const pad = CanvasRenderer.SELECTION_PADDING;
    const b = this.getElementBounds(ctx, element);

    return {
      x: b.x - pad,
      y: b.y - pad,
      width: b.width + pad * 2,
      height: b.height + pad * 2
    };
  }

  getHandlePositions(box: Bounds): Record<ResizeHandle, Point> {
    const { x, y, width, height } = box;

    return {
      nw: { x, y },
      n: { x: x + width / 2, y },
      ne: { x: x + width, y },
      e: { x: x + width, y: y + height / 2 },
      se: { x: x + width, y: y + height },
      s: { x: x + width / 2, y: y + height },
      sw: { x, y: y + height },
      w: { x, y: y + height / 2 }
    };
  }

  // -------------------------
  // Selection
  // -------------------------

  renderSelections(
    ctx: CanvasRenderingContext2D,
    strokes: Stroke[],
    shapes: Shape[],
    zoom: number
  ) {
    for (const stroke of strokes) {
      if (stroke.isSelected && stroke.points.length) {
        this.drawSelectionBox(ctx, this.getSelectionBox(ctx, stroke), zoom);
      }
    }

    for (const shape of shapes) {
      if (shape.isSelected) {
        this.drawSelectionBox(ctx, this.getSelectionBox(ctx, shape), zoom);
      }
    }
  }

  /** Line width, dash pattern and handle size are divided by zoom so the
   *  selection chrome keeps a constant on-screen size. */
  private drawSelectionBox(
    ctx: CanvasRenderingContext2D,
    box: Bounds,
    zoom: number
  ) {
    ctx.save();

    ctx.strokeStyle = '#4f46e5';
    ctx.lineWidth = 1 / zoom;
    ctx.setLineDash([5 / zoom, 4 / zoom]);

    ctx.strokeRect(box.x, box.y, box.width, box.height);

    ctx.setLineDash([]);

    const handleSize = 6 / zoom;

    ctx.fillStyle = '#ffffff';
    ctx.strokeStyle = '#4f46e5';

    for (const handle of Object.values(this.getHandlePositions(box))) {
      ctx.fillRect(
        handle.x - handleSize / 2,
        handle.y - handleSize / 2,
        handleSize,
        handleSize
      );

      ctx.strokeRect(
        handle.x - handleSize / 2,
        handle.y - handleSize / 2,
        handleSize,
        handleSize
      );
    }

    ctx.restore();
  }

  // -------------------------
  // Sticky text layout
  // -------------------------

  /**
   * Word-wraps a sticky's text and returns the layout used by both
   * renderText and renderCaret. Leaves ctx configured (font, center
   * alignment, top baseline) for the caller. Always returns at least one
   * (possibly empty) line so the caret has somewhere to sit.
   */
  private layoutStickyText(ctx: CanvasRenderingContext2D, shape: Shape): StickyLayout {
    const x = Math.min(shape.startPoint.x, shape.endPoint.x);
    const y = Math.min(shape.startPoint.y, shape.endPoint.y);
    const width = Math.abs(shape.endPoint.x - shape.startPoint.x);
    const height = Math.abs(shape.endPoint.y - shape.startPoint.y);

    const maxWidth = width - this.stickyPadding * 2;
    const lineHeight = this.stickyFontSize * 1.2;

    ctx.font = `${this.stickyFontSize}px Excalifont, "Comic Neue", cursive`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';

    const words = (shape.text ?? '').split(' ');
    const lines: string[] = [];
    let currentLine = '';

    for (const word of words) {
      const testLine = currentLine
        ? `${currentLine} ${word}`
        : word;

      if (ctx.measureText(testLine).width <= maxWidth) {
        currentLine = testLine;
      } else {
        if (currentLine) {
          lines.push(currentLine);
        }

        currentLine = word;
      }
    }

    if (currentLine) {
      lines.push(currentLine);
    }

    if (!lines.length) {
      lines.push('');
    }

    const totalHeight = lines.length * lineHeight;

    return {
      lines,
      centerX: x + width / 2,
      startY: y + (height - totalHeight) / 2,
      lineHeight
    };
  }
}