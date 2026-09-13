import { Point } from '../canvas/models';

export function insertText(
  ctx: CanvasRenderingContext2D,
  start: Point,
  text:string
) {
  ctx.beginPath();
  ctx.fillText(text, start.x, start.y);
}