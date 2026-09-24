import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Board, BoardMember, BoardPermission, Invitation } from '../features/whiteboard/canvas/models';
import { Observable } from 'rxjs';

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
getMyInvitations(): Observable<Invitation[]> {
  return this.http.get<Invitation[]>(
    `${this.baseUrl}/invitations`
  );
}

acceptInvitation(
  invitationId: string
): Observable<Invitation> {

  return this.http.post<Invitation>(
    `${this.baseUrl}/invitations/${invitationId}/accept`,
    {}
  );
}

declineInvitation(
  invitationId: string
): Observable<Invitation> {

  return this.http.post<Invitation>(
    `${this.baseUrl}/invitations/${invitationId}/decline`,
    {}
  );
}

sendInvitation(
  boardId: string,
  email: string,
  permission: BoardPermission
): Observable<Invitation> {

  return this.http.post<Invitation>(
    `${this.baseUrl}/invitations/board/${boardId}`,
    {
      email,
      permission
    }
  );
}
removeBoardMember(
  boardId: string,
  memberId: number
): Observable<void> {
  return this.http.delete<void>(
    `${this.baseUrl}/boards/${boardId}/members/${memberId}`
  );
}

updateMemberPermission(
  boardId: string,
  memberId: number,
  permission: BoardPermission
): Observable<void> {
  return this.http.patch<void>(
    `${this.baseUrl}/boards/${boardId}/members`,
    {
      memberId,
      permission
    }
  );
}
  getBoardMembers(
    boardId: string
  ): Observable<BoardMember[]> {

    return this.http.get<BoardMember[]>(
      `${this.baseUrl}/boards/${boardId}/members`
    );
  }
}
