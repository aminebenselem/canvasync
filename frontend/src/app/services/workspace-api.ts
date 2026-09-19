import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Board } from '../features/whiteboard/canvas/models';

@Injectable({
  providedIn: 'root',
})
export class WorkspaceApi {

  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

getUserBoards() {
    return this.http.get<Board[]>(`${this.baseUrl}/boards`);
  }  

deleteBoard(boardId: string) {
    return this.http.delete<Board>(`${this.baseUrl}/boards/${boardId}`);
  }
createBoard(boardName: string)  {
    return this.http.post<Board>(`${this.baseUrl}/boards`, { name: boardName });
  }

}
