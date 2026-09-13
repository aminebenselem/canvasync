import { Point } from '../canvas/models';

export function drawArrow(
  ctx: CanvasRenderingContext2D,
  start: Point,
  end: Point
) {
  const angle = Math.atan2(
    end.y - start.y,
    end.x - start.x
  );

  const arrowSize = 10;

  ctx.beginPath();

  // Main line
  ctx.moveTo(start.x, start.y);
  ctx.lineTo(end.x, end.y);

  // First side of arrowhead
  ctx.moveTo(end.x, end.y);

  ctx.lineTo(
    end.x - arrowSize * Math.cos(angle - Math.PI / 6),
    end.y - arrowSize * Math.sin(angle - Math.PI / 6)
  );

  // Second side of arrowhead
  ctx.moveTo(end.x, end.y);

  ctx.lineTo(
    end.x - arrowSize * Math.cos(angle + Math.PI / 6),
    end.y - arrowSize * Math.sin(angle + Math.PI / 6)
  );

  ctx.stroke();
}