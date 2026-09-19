import { TestBed } from '@angular/core/testing';

import { WhiteboardApi } from './whiteboard-api';

describe('WhiteboardApi', () => {
  let service: WhiteboardApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WhiteboardApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
