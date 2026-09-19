import { TestBed } from '@angular/core/testing';

import { CanvasRenderer } from './canvas-renderer';

describe('CanvasRenderer', () => {
  let service: CanvasRenderer;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CanvasRenderer);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
