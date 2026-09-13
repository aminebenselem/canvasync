import { Injectable } from '@angular/core';
import { Shape, Stroke, Viewport } from './canvas/models';
import { drawRectangle } from './shapes/rectangle';
import { drawEllipse } from './shapes/ellipse';
import { drawArrow } from './shapes/arrow';
import { drawLine } from './shapes/line';

@Injectable({
  providedIn: 'root'
})
export class CanvasRenderer {

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

  }

  renderShapes(ctx: CanvasRenderingContext2D, shapes: Shape[]) {
    for (const shape of shapes) {
      ctx.strokeStyle = shape.color;
      ctx.lineWidth = shape.width;
      switch (shape.type) {
        case 'line':
          drawLine(ctx, shape.start, shape.end);
          break;

        case 'arrow':
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
}