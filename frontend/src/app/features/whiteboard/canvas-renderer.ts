import { Injectable } from '@angular/core';
import { Point, Shape, Stroke, Viewport } from './canvas/models';
import { drawRectangle } from './shapes/rectangle';
import { drawEllipse } from './shapes/ellipse';
import { drawArrow } from './shapes/arrow';
import { drawLine } from './shapes/line';
import { drawStickyNote } from './shapes/stickynote';

@Injectable({
  providedIn: 'root'
})
export class CanvasRenderer {
  private readonly stickyFontSize = 16;

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

  }

  renderShapes(ctx: CanvasRenderingContext2D, shapes: Shape[]) {
    for (const shape of shapes) {
      ctx.strokeStyle = shape.color;
      ctx.lineWidth = shape.width;
      switch (shape.type) {
        case 'sticky':
          drawStickyNote(ctx, shape.start, shape.end);
          break;
        case 'line':
          drawLine(ctx, shape.start, shape.end);
          break;

        case 'arrow':
          ctx.lineWidth = 2;
          drawArrow(ctx, shape.start, shape.end);
          break;
        case 'rectangle':
          drawRectangle(ctx, shape.start, shape.end);
          break;
        case 'ellipse':
          drawEllipse(
            ctx,
            shape.start,
            shape.end
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
        ctx.textBaseline = 'top';
        ctx.fillText(shape.text, shape.start.x, shape.start.y);
      }

      if (shape.type === 'sticky' && shape.text) {
        const x = Math.min(shape.start.x, shape.end.x);
        const y = Math.min(shape.start.y, shape.end.y);
        const width = Math.abs(shape.end.x - shape.start.x);
        const height = Math.abs(shape.end.y - shape.start.y);

        const padding = 10;
        const maxWidth = width - padding * 2;
        const lineHeight = this.stickyFontSize * 1.2;

        ctx.fillStyle = '#3a3a3a';
        ctx.font = `${this.stickyFontSize}px Excalifont, "Comic Neue", cursive`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';

        const words = shape.text.split(' ');
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

        const totalHeight = lines.length * lineHeight;
        const startY = y + (height - totalHeight) / 2;

        lines.forEach((line, index) => {
          ctx.fillText(
            line,
            x + width / 2,
            startY + index * lineHeight
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

    const text = shape.text ?? '';
    const fontSize = shape.type === 'sticky' ? this.stickyFontSize : shape.width;

    ctx.font = `${fontSize}px Excalifont, "Comic Neue", cursive`;
    ctx.textBaseline = 'top';

    const textWidth = ctx.measureText(text).width;

    const origin = shape.type === 'sticky'
      ? this.stickyTextOrigin(shape)
      : { x: shape.start.x, y: shape.start.y };

    const x = origin.x + textWidth;
    const y = origin.y;

    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(x, y + fontSize);

    ctx.strokeStyle = shape.color;
    ctx.lineWidth = 1;
    ctx.stroke();
  }

  /**
   * Normalizes a sticky's start/end into its visual top-left corner
   * (matching drawStickyNote's own Math.min/abs normalization) and
   * applies a small inset so text doesn't touch the note's edges.
   */
  private stickyTextOrigin(shape: Shape): Point {
    const padding = 10;

    const x = Math.min(shape.start.x, shape.end.x);
    const y = Math.min(shape.start.y, shape.end.y);

    return { x: x + padding, y: y + padding };
  }
}