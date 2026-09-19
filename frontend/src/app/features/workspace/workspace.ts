import { ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';

import { Board } from '../whiteboard/canvas/models';
import { WorkspaceApi } from '../../services/workspace-api';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-workspace',
  imports: [DatePipe ,FormsModule],
  templateUrl: './workspace.html',
  styleUrl: './workspace.css',
})
export class Workspace implements OnInit {

  boards = signal<Board[]>([]);

  showCreateDialog = false;
  newBoardName = '';

  constructor(
    private workspaceApi: WorkspaceApi,
    private router: Router,
  ) {}

 ngOnInit() {
    this.workspaceApi.getUserBoards().subscribe({
      next: boards => {
        this.boards.set(boards);

      }
    });
  }

  openBoard(boardId: string) {
    this.router.navigate(['/whiteboard', boardId]);
  }

  deleteBoard(boardId: string) {
    this.workspaceApi.deleteBoard(boardId).subscribe({
      next: () => {
        this.boards.update(boards => boards.filter(board => board.id !== boardId));
      }
    });
  }

  openCreateDialog() {
    this.newBoardName = '';
    this.showCreateDialog = true;
  }

  closeCreateDialog() {
    this.showCreateDialog = false;
    this.newBoardName = '';
  }

  createBoard() {
    const name = this.newBoardName.trim();

    if (!name) {
      return;
    }

    this.workspaceApi.createBoard(name).subscribe({
      next: newBoard => {
        this.boards.update(boards => [...boards, newBoard]);
        this.closeCreateDialog();
      }
    });
  }
}