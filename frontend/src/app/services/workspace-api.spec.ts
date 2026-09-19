import { TestBed } from '@angular/core/testing';

import { WorkspaceApi } from './workspace-api';

describe('WorkspaceApi', () => {
  let service: WorkspaceApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WorkspaceApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
