import { Point } from "../canvas/models";


export function drawRectangle(
  ctx: CanvasRenderingContext2D,
  start: Point,
  end: Point
) {
  const width = end.x - start.x;
  const height = end.y - start.y;

  ctx.beginPath();
  ctx.rect(
    start.x,
    start.y,
    width,
    height
  );
  ctx.stroke();
}