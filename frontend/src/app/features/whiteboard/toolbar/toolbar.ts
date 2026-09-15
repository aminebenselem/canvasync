import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, signal } from '@angular/core';



export type ToolType = 'select' | 'pen' | 'eraser' | 'rectangle' | 'ellipse' | 'line' | 'arrow' | 'text' | 'sticky' | 'pan';
interface Tool {
  id: ToolType;
  label: string;
  icon: string; // svg path data
}
@Component({
  selector: 'app-toolbar',
  imports: [CommonModule],
  templateUrl: './toolbar.html',
  styleUrl: './toolbar.css',
})
export class Toolbar {
 @Output() toolChange = new EventEmitter<ToolType>();
  @Output() colorChange = new EventEmitter<string>();
  @Output() strokeWidthChange = new EventEmitter<number>();
  @Output() fontSizeChange = new EventEmitter<number>();
  @Output() undo = new EventEmitter<void>();
  @Output() redo = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();
  @Output() zoomChange = new EventEmitter<number>();
   
  @Input() canUndo = false;
  @Input() canRedo = false;

  activeTool = signal<ToolType>('select');
  activeColor = signal<string>('#1e1e1e');
  strokeWidth = signal<number>(3);
  fontSize = signal<number>(20);
  zoom = signal<number>(100);
  showColorPicker = signal(false);

  tools: Tool[] = [
    { id: 'select', label: 'Select', icon: 'M4 4l7 16 2-7 7-2z' },
    { id: 'pen', label: 'Pen', icon: 'M15 5l4 4L7 21H3v-4z' },
    { id: 'eraser', label: 'Eraser', icon: 'M3 17l6 4h4l8-8-6-6-8 8z' },
    { id: 'rectangle', label: 'Rectangle', icon: 'M4 4h16v16H4z' },
    { id: 'ellipse', label: 'Ellipse', icon: 'M12 4a8 8 0 100 16 8 8 0 000-16z' },
    { id: 'line', label: 'Line', icon: 'M4 20L20 4' },
    { id: 'arrow', label: 'Arrow', icon: 'M4 20L20 4M20 4h-6M20 4v6' },
    { id: 'text', label: 'Text', icon: 'M5 4h14M12 4v16' },
   // { id: 'sticky', label: 'Sticky note', icon: 'M4 4h16v16H4zM14 4v6h6' },
    { id: 'pan', label: 'Pan', icon: 'M7 11V5a2 2 0 0 1 4 0v6M11 11V3a2 2 0 0 1 4 0v8M15 11V5a2 2 0 0 1 4 0v8M7 9a2 2 0 0 0-4 0v3c0 5 4 9 9 9h1a6 6 0 0 0 6-6' },
  ];

  palette = ['#1e1e1e', '#e03131', '#2f9e44', '#1971c2', '#f08c00', '#ffffff'];

  // Matches Excalidraw's own S/M/L/XL font-size presets
  fontSizes: number[] = [16, 20, 28, 36,48, 64];

  selectTool(id: ToolType) {
    this.activeTool.set(id);
    this.toolChange.emit(id);
  }

  selectColor(color: string) {
    this.activeColor.set(color);
    this.colorChange.emit(color);
    this.showColorPicker.set(false);
  }

  setStrokeWidth(value: number) {
    this.strokeWidth.set(value);
    this.strokeWidthChange.emit(value);
  }

  setFontSize(value: number) {
    this.fontSize.set(value);
    this.fontSizeChange.emit(value);
  }

  zoomIn() {
    const z = Math.min(400, this.zoom() + 10);
    this.zoom.set(z);
    this.zoomChange.emit(z);
  }

  zoomOut() {
    const z = Math.max(10, this.zoom() - 10);
    this.zoom.set(z);
    this.zoomChange.emit(z);
  }

  resetZoom() {
    this.zoom.set(100);
    this.zoomChange.emit(100);
  }
}