import { Point } from "../canvas/models";

export function drawEllipse(
  ctx: CanvasRenderingContext2D,
  start: Point,
  end: Point
) {
  const centerX = (start.x + end.x) / 2;
  const centerY = (start.y + end.y) / 2;

  const radiusX = Math.abs(end.x - start.x) / 2;
  const radiusY = Math.abs(end.y - start.y) / 2;

  ctx.beginPath();

  ctx.ellipse(
    centerX,
    centerY,
    radiusX,
    radiusY,
    0,
    0,
    Math.PI * 2
  );

  ctx.stroke();
}