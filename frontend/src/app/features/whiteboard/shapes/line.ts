import { Point } from '../canvas/models';

export function drawLine(
  ctx: CanvasRenderingContext2D,
  start: Point,
  end: Point
) {
  ctx.beginPath();

  ctx.moveTo(start.x, start.y);
  ctx.lineTo(end.x, end.y);

  ctx.stroke();
}