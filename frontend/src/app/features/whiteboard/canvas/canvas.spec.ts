// import { ComponentFixture, TestBed } from '@angular/core/testing';
// import { ActivatedRoute, convertToParamMap } from '@angular/router';
// import { of } from 'rxjs';

// import { Canvas } from './canvas';
// import { Point, Shape, Stroke } from './models';
// import { CanvasRenderer, ResizeHandle } from '../../../services/canvas-renderer';
// import { WhiteboardApi } from '../../../services/whiteboard-api';

// // -------------------------
// // Helpers
// // -------------------------

// const mouse = (x: number, y: number): MouseEvent =>
//   ({ offsetX: x, offsetY: y, button: 0 }) as MouseEvent;

// const key = (k: string, init: KeyboardEventInit = {}) =>
//   new KeyboardEvent('keydown', { key: k, ...init });

// const makeShape = (overrides: Partial<Shape> & Pick<Shape, 'id' | 'type'>): Shape =>
//   ({
//     startPoint: { x: 0, y: 0 },
//     endPoint: { x: 0, y: 0 },
//     color: '#000000',
//     width: 1,
//     text: '',
//     isSelected: false,
//     ...overrides
//   }) as Shape;

// const makeRect = (id = 'r1'): Shape =>
//   makeShape({
//     id,
//     type: 'rectangle',
//     startPoint: { x: 100, y: 100 },
//     endPoint: { x: 200, y: 200 }
//   });

// const makeText = (id = 't1', text = 'Hello'): Shape =>
//   makeShape({
//     id,
//     type: 'text',
//     text,
//     width: 20,
//     startPoint: { x: 50, y: 50 },
//     endPoint: { x: 50, y: 50 }
//   });

// describe('Canvas', () => {
//   let fixture: ComponentFixture<Canvas>;
//   let component: Canvas;
//   let api: jasmine.SpyObj<WhiteboardApi>;
//   let renderer: CanvasRenderer;

//   const click = (x: number, y: number) => {
//     component.onMouseDown(mouse(x, y));
//     component.onMouseUp(mouse(x, y));
//   };

//   const drag = (from: Point, to: Point) => {
//     component.onMouseDown(mouse(from.x, from.y));
//     component.onMouseMove(mouse(to.x, to.y));
//     component.onMouseUp(mouse(to.x, to.y));
//   };

//   const type = (text: string) => {
//     for (const ch of text) {
//       component.onKeyDown(key(ch));
//     }
//   };

//   const handleAt = (shape: Shape, name: ResizeHandle): Point => {
//     const ctx = document.createElement('canvas').getContext('2d')!;
//     const box = renderer.getSelectionBox(ctx, shape);

//     return renderer.getHandlePositions(box)[name];
//   };

//   const lastUpdateShapeDto = () =>
//     api.updateShape.calls.mostRecent().args[2] as unknown as Record<string, unknown>;

//   beforeEach(async () => {
//     api = jasmine.createSpyObj<WhiteboardApi>('WhiteboardApi', [
//       'getBoardElements',
//       'createShape',
//       'createStroke',
//       'updateShape',
//       'updateStroke',
//       'deleteElement',
//       'deleteAllElements'
//     ]);

//     api.getBoardElements.and.returnValue(of([]));
//     api.createShape.and.returnValue(of({ id: 'srv-1', type: 'TEXT', data: {} } as any));
//     api.createStroke.and.returnValue(of({ id: 'srv-2', type: 'STROKE', data: {} } as any));
//     api.updateShape.and.returnValue(of({} as any));
//     api.updateStroke.and.returnValue(of({} as any));
//     api.deleteElement.and.returnValue(of(undefined as any));
//     api.deleteAllElements.and.returnValue(of(undefined as any));

//     await TestBed.configureTestingModule({
//       imports: [Canvas],
//       providers: [
//         { provide: WhiteboardApi, useValue: api },
//         {
//           provide: ActivatedRoute,
//           useValue: { snapshot: { paramMap: convertToParamMap({ boardId: 'b1' }) } }
//         }
//       ]
//     }).compileComponents();

//     fixture = TestBed.createComponent(Canvas);
//     component = fixture.componentInstance;
//     renderer = TestBed.inject(CanvasRenderer);

//     fixture.detectChanges();
//   });

//   afterEach(() => {
//     // Stop the caret interval if a test left a text edit open.
//     if (component.isEditingText) {
//       component.onKeyDown(key('Escape'));
//     }
//   });

//   // -------------------------
//   // Loading
//   // -------------------------

//   describe('addElementToCanvas', () => {
//     const data = {
//       text: '',
//       color: '#000000',
//       width: 1,
//       startPoint: { x: 0, y: 0 },
//       endPoint: { x: 10, y: 10 }
//     };

//     it('falls back to the element-level type when data.type is missing (post-PATCH rows)', () => {
//       component.addElementToCanvas({ id: 'e1', type: 'ELLIPSE', data } as any);

//       expect(component.currentCanvas.shapes[0].type).toBe('ellipse');
//     });

//     it('lowercases an uppercase data.type', () => {
//       component.addElementToCanvas({ id: 'e1', type: 'RECTANGLE', data: { ...data, type: 'RECTANGLE' } } as any);

//       expect(component.currentCanvas.shapes[0].type).toBe('rectangle');
//     });

//     it('keeps a valid lowercase data.type', () => {
//       component.addElementToCanvas({ id: 'e1', type: 'LINE', data: { ...data, type: 'line' } } as any);

//       expect(component.currentCanvas.shapes[0].type).toBe('line');
//     });

//     it('routes STROKE elements to strokes', () => {
//       component.addElementToCanvas({ id: 'k1', type: 'STROKE', data: { color: '#000', width: 1, points: [] } } as any);

//       expect(component.currentCanvas.strokes.length).toBe(1);
//       expect(component.currentCanvas.shapes.length).toBe(0);
//     });
//   });

//   // -------------------------
//   // Selection
//   // -------------------------

//   describe('selection', () => {
//     it('selects a rectangle when its outline is clicked', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       click(100, 150);

//       expect(rect.isSelected).toBeTrue();
//     });

//     it('selects a sticky when its interior is clicked', () => {
//       const sticky = makeShape({
//         id: 'st1',
//         type: 'sticky',
//         startPoint: { x: 50, y: 50 },
//         endPoint: { x: 150, y: 120 }
//       });
//       component.currentCanvas.shapes.push(sticky);

//       click(100, 80);

//       expect(sticky.isSelected).toBeTrue();
//     });

//     it('clears the selection when empty canvas is clicked', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       click(100, 150);
//       click(600, 600);

//       expect(rect.isSelected).toBeFalse();
//     });

//     it('keeps a single selection: selecting another element deselects the first', () => {
//       const a = makeRect('a');
//       const b = makeShape({
//         id: 'b',
//         type: 'rectangle',
//         startPoint: { x: 400, y: 400 },
//         endPoint: { x: 500, y: 500 }
//       });
//       component.currentCanvas.shapes.push(a, b);

//       click(100, 150);
//       click(400, 450);

//       expect(a.isSelected).toBeFalse();
//       expect(b.isSelected).toBeTrue();
//     });

//     it('clears the selection on Escape', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       click(100, 150);
//       component.onKeyDown(key('Escape'));

//       expect(rect.isSelected).toBeFalse();
//     });

//     it('clears the selection when switching away from the select tool', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       click(100, 150);
//       component.onToolChange('pen');

//       expect(rect.isSelected).toBeFalse();
//     });
//   });

//   // -------------------------
//   // Move
//   // -------------------------

//   describe('move', () => {
//     it('translates a shape by the drag delta and persists it once', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       drag({ x: 100, y: 150 }, { x: 130, y: 170 });

//       expect(rect.startPoint).toEqual({ x: 130, y: 120 });
//       expect(rect.endPoint).toEqual({ x: 230, y: 220 });
//       expect(api.updateShape).toHaveBeenCalledTimes(1);
//       expect(api.updateShape).toHaveBeenCalledWith(
//         'b1',
//         'r1',
//         jasmine.objectContaining({
//           startPoint: { x: 130, y: 120 },
//           endPoint: { x: 230, y: 220 }
//         })
//       );
//     });

//     it('never sends type/shapeType in the shape update payload', () => {
//       component.currentCanvas.shapes.push(makeRect());

//       drag({ x: 100, y: 150 }, { x: 130, y: 170 });

//       const dto = lastUpdateShapeDto();
//       expect('type' in dto).toBeFalse();
//       expect('shapeType' in dto).toBeFalse();
//     });

//     it('translates every point of a stroke and calls updateStroke', () => {
//       const stroke = {
//         id: 's1',
//         color: '#000000',
//         width: 2,
//         isSelected: false,
//         points: [{ x: 0, y: 0 }, { x: 100, y: 0 }]
//       } as Stroke;
//       component.currentCanvas.strokes.push(stroke);

//       drag({ x: 50, y: 0 }, { x: 60, y: 20 });

//       expect(stroke.points).toEqual([{ x: 10, y: 20 }, { x: 110, y: 20 }]);
//       expect(api.updateStroke).toHaveBeenCalledWith(
//         'b1',
//         's1',
//         jasmine.objectContaining({ points: [{ x: 10, y: 20 }, { x: 110, y: 20 }] })
//       );
//       expect(api.updateShape).not.toHaveBeenCalled();
//     });

//     it('treats a plain click as a click: no persist, no undo entry', () => {
//       component.currentCanvas.shapes.push(makeRect());

//       click(100, 150);

//       expect(api.updateShape).not.toHaveBeenCalled();
//       expect(component.currentCanvas.undoStack.length).toBe(0);
//     });

//     it('ignores sub-3px jitter', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       drag({ x: 100, y: 150 }, { x: 101, y: 151 });

//       expect(rect.startPoint).toEqual({ x: 100, y: 100 });
//       expect(api.updateShape).not.toHaveBeenCalled();
//     });

//     it('does not persist a shape that has no server id yet', () => {
//       component.currentCanvas.shapes.push(makeRect(''));

//       drag({ x: 100, y: 150 }, { x: 130, y: 170 });

//       expect(api.updateShape).not.toHaveBeenCalled();
//     });

//     it('pushes one undo entry and undo restores the original position', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);

//       drag({ x: 100, y: 150 }, { x: 130, y: 170 });
//       expect(component.currentCanvas.undoStack.length).toBe(1);

//       component.onUndo();

//       const restored = component.currentCanvas.shapes[0];
//       expect(restored.startPoint).toEqual({ x: 100, y: 100 });
//       expect(restored.endPoint).toEqual({ x: 200, y: 200 });
//       expect(restored.isSelected).toBeFalse();
//     });
//   });

//   // -------------------------
//   // Resize
//   // -------------------------

//   describe('resize', () => {
//     it('drags the se handle: start stays, end follows the pointer (minus padding)', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);
//       click(100, 150);

//       const se = handleAt(rect, 'se'); // (206, 206)
//       drag(se, { x: se.x + 100, y: se.y + 50 });

//       expect(rect.startPoint).toEqual({ x: 100, y: 100 });
//       expect(rect.endPoint).toEqual({ x: 300, y: 250 });
//       expect(api.updateShape).toHaveBeenCalledTimes(1);
//       expect(component.currentCanvas.undoStack.length).toBe(1);
//     });

//     it('drags the nw handle: end stays fixed', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);
//       click(100, 150);

//       const nw = handleAt(rect, 'nw'); // (94, 94)
//       drag(nw, { x: nw.x - 40, y: nw.y - 20 });

//       expect(rect.startPoint.x).toBeCloseTo(60);
//       expect(rect.startPoint.y).toBeCloseTo(80);
//       expect(rect.endPoint.x).toBeCloseTo(200);
//       expect(rect.endPoint.y).toBeCloseTo(200);
//     });

//     it('mirrors when a handle is dragged past the opposite edge', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);
//       click(100, 150);

//       const se = handleAt(rect, 'se');
//       drag(se, { x: 56, y: se.y }); // right edge -> 50, left edge stays 100

//       expect(rect.startPoint.x).toBeCloseTo(100);
//       expect(rect.endPoint.x).toBeCloseTo(50);
//       expect(rect.endPoint.y).toBeCloseTo(200);
//     });

//     it('locks a zero-extent axis (vertical line cannot be widened)', () => {
//       const line = makeShape({
//         id: 'l1',
//         type: 'line',
//         startPoint: { x: 100, y: 100 },
//         endPoint: { x: 100, y: 200 }
//       });
//       component.currentCanvas.shapes.push(line);
//       click(100, 150);

//       const east = handleAt(line, 'e');
//       drag(east, { x: east.x + 100, y: east.y });

//       expect(line.startPoint.x).toBe(100);
//       expect(line.endPoint.x).toBe(100);
//     });

//     it('scales stroke points proportionally', () => {
//       const stroke = {
//         id: 's1',
//         color: '#000000',
//         width: 2,
//         isSelected: false,
//         points: [{ x: 0, y: 0 }, { x: 100, y: 100 }]
//       } as Stroke;
//       component.currentCanvas.strokes.push(stroke);
//       click(50, 50);

//       const ctx = document.createElement('canvas').getContext('2d')!;
//       const se = renderer.getHandlePositions(renderer.getSelectionBox(ctx, stroke)).se; // (106, 106)
//       drag(se, { x: se.x + 100, y: se.y + 100 }); // bounds become 200 x 200

//       expect(stroke.points[0].x).toBeCloseTo(0);
//       expect(stroke.points[1].x).toBeCloseTo(200);
//       expect(stroke.points[1].y).toBeCloseTo(200);
//       expect(api.updateStroke).toHaveBeenCalledTimes(1);
//     });

//     it('scales text font size from a corner handle and keeps the anchor', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);
//       click(55, 55);

//       const se = handleAt(text, 'se');
//       drag(se, { x: se.x + 100, y: se.y + 100 });

//       expect(text.width).toBeGreaterThan(20);
//       expect(text.startPoint).toEqual({ x: 50, y: 50 });
//     });

//     it('never shrinks text below 8px', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);
//       click(55, 55);

//       const se = handleAt(text, 'se');
//       const nw = handleAt(text, 'nw');
//       drag(se, nw);

//       expect(text.width).toBe(8);
//     });
//   });

//   // -------------------------
//   // Text editing
//   // -------------------------

//   describe('editing existing text', () => {
//     it('enters edit mode on double-click and selects the shape', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);

//       component.onDoubleClick(mouse(55, 55));

//       expect(component.isEditingText).toBeTrue();
//       expect(component.currentShape).toBe(text);
//       expect(text.isSelected).toBeTrue();
//     });

//     it('enters edit mode on Enter when a text is selected', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);
//       click(55, 55);

//       component.onKeyDown(key('Enter'));

//       expect(component.isEditingText).toBeTrue();
//       expect(component.currentShape).toBe(text);
//     });

//     it('does not enter edit mode on Enter for a rectangle', () => {
//       component.currentCanvas.shapes.push(makeRect());
//       click(100, 150);

//       component.onKeyDown(key('Enter'));

//       expect(component.isEditingText).toBeFalse();
//     });

//     it('commits an edit with one PATCH, one undo entry and no create', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);

//       component.onDoubleClick(mouse(55, 55));
//       type('!');
//       component.onKeyDown(key('Escape'));

//       expect(text.text).toBe('Hello!');
//       expect(component.isEditingText).toBeFalse();
//       expect(api.updateShape).toHaveBeenCalledTimes(1);
//       expect(api.updateShape).toHaveBeenCalledWith('b1', 't1', jasmine.objectContaining({ text: 'Hello!' }));
//       expect(api.createShape).not.toHaveBeenCalled();
//       expect(component.currentCanvas.undoStack.length).toBe(1);
//       expect(text.isSelected).toBeTrue();
//     });

//     it('does not persist or push undo when the text is unchanged', () => {
//       component.currentCanvas.shapes.push(makeText());

//       component.onDoubleClick(mouse(55, 55));
//       component.onKeyDown(key('Escape'));

//       expect(api.updateShape).not.toHaveBeenCalled();
//       expect(component.currentCanvas.undoStack.length).toBe(0);
//     });

//     it('deletes the shape when its text is emptied', () => {
//       component.currentCanvas.shapes.push(makeText());

//       component.onDoubleClick(mouse(55, 55));
//       for (let i = 0; i < 5; i++) {
//         component.onKeyDown(key('Backspace'));
//       }
//       component.onKeyDown(key('Escape'));

//       expect(component.currentCanvas.shapes.length).toBe(0);
//       expect(api.deleteElement).toHaveBeenCalledWith('b1', 't1');
//       expect(api.updateShape).not.toHaveBeenCalled();
//       expect(component.currentCanvas.undoStack.length).toBe(1);
//     });

//     it('ignores Ctrl/Cmd combinations instead of typing the letter', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);

//       component.onDoubleClick(mouse(55, 55));
//       component.onKeyDown(key('z', { ctrlKey: true }));
//       component.onKeyDown(key('v', { metaKey: true }));

//       expect(text.text).toBe('Hello');
//     });

//     it('commits when the canvas is clicked outside the text', () => {
//       const text = makeText();
//       component.currentCanvas.shapes.push(text);

//       component.onDoubleClick(mouse(55, 55));
//       type('!');
//       component.onMouseDown(mouse(600, 600));

//       expect(component.isEditingText).toBeFalse();
//       expect(api.updateShape).toHaveBeenCalledTimes(1);
//     });

//     it('keeps editing when the click lands inside the text', () => {
//       component.currentCanvas.shapes.push(makeText());

//       component.onDoubleClick(mouse(55, 55));
//       component.onMouseDown(mouse(56, 56));

//       expect(component.isEditingText).toBeTrue();
//     });

//     it('edits a sticky on double-click', () => {
//       const sticky = makeShape({
//         id: 'st1',
//         type: 'sticky',
//         startPoint: { x: 50, y: 50 },
//         endPoint: { x: 200, y: 150 }
//       });
//       component.currentCanvas.shapes.push(sticky);

//       component.onDoubleClick(mouse(100, 100));
//       type('note');
//       component.onKeyDown(key('Escape'));

//       expect(sticky.text).toBe('note');
//       expect(api.updateShape).toHaveBeenCalledTimes(1);
//     });
//   });

//   describe('creating new text', () => {
//     it('creates exactly one shape locally and adopts the server id (no duplicate)', () => {
//       component.onDoubleClick(mouse(400, 400));
//       expect(component.currentCanvas.shapes.length).toBe(1);
//       expect(component.currentCanvas.shapes[0].id).toBe('');

//       type('Hi');
//       component.onKeyDown(key('Escape'));

//       expect(api.createShape).toHaveBeenCalledTimes(1);
//       expect(api.createShape).toHaveBeenCalledWith('b1', jasmine.objectContaining({ text: 'Hi' }));
//       expect(component.currentCanvas.shapes.length).toBe(1);
//       expect(component.currentCanvas.shapes[0].id).toBe('srv-1');
//     });

//     it('discards empty new text without hitting the backend or leaving an undo entry', () => {
//       component.onDoubleClick(mouse(400, 400));
//       component.onKeyDown(key('Escape'));

//       expect(component.currentCanvas.shapes.length).toBe(0);
//       expect(api.createShape).not.toHaveBeenCalled();
//       expect(component.currentCanvas.undoStack.length).toBe(0);
//     });
//   });

//   // -------------------------
//   // Eraser interaction with selection
//   // -------------------------

//   describe('eraser', () => {
//     it('clears the selection when the selected shape is erased', () => {
//       const rect = makeRect();
//       component.currentCanvas.shapes.push(rect);
//       click(100, 150);
//       expect(rect.isSelected).toBeTrue();

//       component.onToolChange('eraser');
//       component.onMouseDown(mouse(100, 150));
//       component.onMouseUp(mouse(100, 150));

//       expect(component.currentCanvas.shapes.length).toBe(0);
//       expect(api.deleteElement).toHaveBeenCalledWith('b1', 'r1');
//     });

//     it('can erase a sticky by clicking inside it', () => {
//       component.currentCanvas.shapes.push(
//         makeShape({
//           id: 'st1',
//           type: 'sticky',
//           startPoint: { x: 50, y: 50 },
//           endPoint: { x: 150, y: 120 }
//         })
//       );

//       component.onToolChange('eraser');
//       component.onMouseDown(mouse(100, 80));
//       component.onMouseUp(mouse(100, 80));

//       expect(component.currentCanvas.shapes.length).toBe(0);
//     });
//   });
// });