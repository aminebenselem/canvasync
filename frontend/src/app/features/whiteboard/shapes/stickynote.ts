import { Point } from "../canvas/models";

export function drawStickyNote(
  ctx: CanvasRenderingContext2D,
  start: Point,
  end: Point
) {
  const x = Math.min(start.x, end.x);
  const y = Math.min(start.y, end.y);
  const width = Math.abs(end.x - start.x);
  const height = Math.abs(end.y - start.y);

  const fold = Math.min(18, width / 4, height / 4);

  ctx.save();

  // Drop shadow for the whole note (lifts it off the canvas)
  ctx.save();
  ctx.shadowColor = 'rgba(0, 0, 0, 0.25)';
  ctx.shadowBlur = 12;
  ctx.shadowOffsetX = 3;
  ctx.shadowOffsetY = 4;

  // Note body with a subtle top-to-bottom gradient instead of flat fill
  const bodyGradient = ctx.createLinearGradient(x, y, x, y + height);
  bodyGradient.addColorStop(0, '#fff9b0');
  bodyGradient.addColorStop(1, '#fff27a');
  ctx.fillStyle = bodyGradient;

  // Draw the note as a path that already excludes the folded corner,
  // so the shadow silhouette matches the folded shape
  ctx.beginPath();
  ctx.moveTo(x, y);
  ctx.lineTo(x + width - fold, y);
  ctx.lineTo(x + width, y + fold);
  ctx.lineTo(x + width, y + height);
  ctx.lineTo(x, y + height);
  ctx.closePath();
  ctx.fill();
  ctx.restore(); // drop shadow off for the rest

  // Thin border, slightly warmer than before
  ctx.strokeStyle = 'rgba(200, 180, 60, 0.6)';
  ctx.lineWidth = 1;
  ctx.stroke();

  // Small shadow cast by the fold onto the note underneath it
  ctx.save();
  ctx.shadowColor = 'rgba(0, 0, 0, 0.2)';
  ctx.shadowBlur = 6;
  ctx.beginPath();
  ctx.moveTo(x + width - fold, y);
  ctx.lineTo(x + width, y + fold);
  ctx.lineTo(x + width - fold, y + fold);
  ctx.closePath();
  ctx.fillStyle = 'rgba(0, 0, 0, 0.06)';
  ctx.fill();
  ctx.restore();

  // The fold itself, with its own gradient so it reads as curled paper
  const foldGradient = ctx.createLinearGradient(
    x + width - fold, y,
    x + width, y + fold
  );
  foldGradient.addColorStop(0, '#fdf1a0');
  foldGradient.addColorStop(1, '#e8d873');

  ctx.beginPath();
  ctx.moveTo(x + width - fold, y);
  ctx.lineTo(x + width, y + fold);
  ctx.lineTo(x + width - fold, y + fold);
  ctx.closePath();
  ctx.fillStyle = foldGradient;
  ctx.fill();

  // Faint crease line under the fold
  ctx.strokeStyle = 'rgba(150, 130, 40, 0.4)';
  ctx.lineWidth = 0.75;
  ctx.beginPath();
  ctx.moveTo(x + width - fold, y);
  ctx.lineTo(x + width - fold, y + fold);
  ctx.stroke();

  ctx.restore();
}