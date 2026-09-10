import { AfterViewInit, Component, ElementRef, ViewChild } from '@angular/core';

@Component({
  selector: 'app-canvas',
  imports: [],
  templateUrl: './canvas.html',
  styleUrl: './canvas.css',
})

export class Canvas implements AfterViewInit {
  @ViewChild('canvas', { static: true })
  canvas!: ElementRef<HTMLCanvasElement>;




 ngAfterViewInit() {
    const ctx = this.canvas.nativeElement.getContext('2d');

    if (!ctx) {
      return;
    }

    ctx.beginPath();
    ctx.moveTo(100, 100);
    ctx.lineTo(300, 200);
        ctx.lineTo(500, 200);
        ctx.lineTo(500, 200);
        ctx.arcTo(600, 200, 600, 300, 100);
    ctx.stroke();

  }



}
