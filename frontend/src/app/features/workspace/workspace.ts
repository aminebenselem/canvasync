import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import {
  Board,
  Invitation
} from '../whiteboard/canvas/models';

import { WorkspaceApi } from '../../services/workspace-api';

@Component({
  selector: 'app-workspace',
  imports: [DatePipe, FormsModule],
  templateUrl: './workspace.html',
  styleUrl: './workspace.css',
})
export class Workspace implements OnInit {

  // =========================
  // Boards
  // =========================

  boards = signal<Board[]>([]);

  boardsLoading = signal(false);
  boardsError = signal<string | null>(null);

  // =========================
  // Create board
  // =========================

  showCreateDialog = false;
  newBoardName = '';

  createBoardLoading = signal(false);
  createBoardError = signal<string | null>(null);

  // =========================
  // Invitations
  // =========================

  invitations = signal<Invitation[]>([]);

  invitationsLoading = signal(false);
  invitationsError = signal<string | null>(null);

  invitationActionLoading = signal<{
    id: string;
    type: 'accept' | 'decline';
  } | null>(null);

  showInvitations = false;

  constructor(
    private workspaceApi: WorkspaceApi,
    private router: Router
  ) {}

  // =========================
  // Lifecycle
  // =========================

  ngOnInit(): void {
    this.loadBoards();
    this.loadInvitations();
  }

  // =========================
  // Boards
  // =========================

  loadBoards(): void {
    this.boardsLoading.set(true);
    this.boardsError.set(null);

    this.workspaceApi.getUserBoards().subscribe({
      next: boards => {
        this.boards.set(boards);
        this.boardsLoading.set(false);
      },

      error: error => {
        console.error('Failed to load boards:', error);

        this.boardsLoading.set(false);
        this.boardsError.set('Failed to load your boards.');
      }
    });
  }

  openBoard(boardId: string): void {
    this.router.navigate(['/whiteboard', boardId]);
  }

  deleteBoard(boardId: string): void {
    this.workspaceApi.deleteBoard(boardId).subscribe({
      next: () => {
        this.boards.update(
          boards => boards.filter(board => board.id !== boardId)
        );
      },

      error: error => {
        console.error('Failed to delete board:', error);
      }
    });
  }

  // =========================
  // Create board
  // =========================

  openCreateDialog(): void {
    this.newBoardName = '';
    this.createBoardError.set(null);
    this.showCreateDialog = true;
  }

  closeCreateDialog(): void {
    this.showCreateDialog = false;
    this.newBoardName = '';
    this.createBoardError.set(null);
  }

  createBoard(): void {
    const name = this.newBoardName.trim();

    if (!name) {
      this.createBoardError.set('Board name is required.');
      return;
    }

    this.createBoardLoading.set(true);
    this.createBoardError.set(null);

    this.workspaceApi.createBoard(name).subscribe({
      next: newBoard => {
        this.boards.update(
          boards => [...boards, newBoard]
        );

        this.createBoardLoading.set(false);
        this.closeCreateDialog();
      },

      error: error => {
        console.error('Failed to create board:', error);

        this.createBoardLoading.set(false);
        this.createBoardError.set(
          'Failed to create the board. Please try again.'
        );
      }
    });
  }

  // =========================
  // Invitations
  // =========================

  loadInvitations(): void {
    this.invitationsLoading.set(true);
    this.invitationsError.set(null);

    this.workspaceApi.getMyInvitations().subscribe({
      next: invitations => {
        this.invitations.set(
          invitations.filter(
            invitation => invitation.status === 'PENDING'
          )
        );

        this.invitationsLoading.set(false);
      },

      error: error => {
        console.error('Failed to load invitations:', error);

        this.invitationsLoading.set(false);
        this.invitationsError.set(
          'Failed to load your invitations.'
        );
      }
    });
  }

  openInvitations(): void {
    this.showInvitations = true;
    this.loadInvitations();
  }

  closeInvitations(): void {
    this.showInvitations = false;
  }

  // =========================
  // Accept invitation
  // =========================

  acceptInvitation(invitationId: string): void {
    this.invitationActionLoading.set({
      id: invitationId,
      type: 'accept'
    });

    this.workspaceApi.acceptInvitation(invitationId).subscribe({
      next: () => {

        this.invitations.update(
          invitations =>
            invitations.filter(
              invitation => invitation.id !== invitationId
            )
        );

        this.loadBoards();

        this.invitationActionLoading.set(null);
      },

      error: error => {
        console.error(
          'Failed to accept invitation:',
          error
        );

        this.invitationActionLoading.set(null);
      }
    });
  }

  // =========================
  // Decline invitation
  // =========================

  declineInvitation(invitationId: string): void {
    this.invitationActionLoading.set({
      id: invitationId,
      type: 'decline'
    });

    this.workspaceApi.declineInvitation(invitationId).subscribe({
      next: () => {

        this.invitations.update(
          invitations =>
            invitations.filter(
              invitation => invitation.id !== invitationId
            )
        );

        this.invitationActionLoading.set(null);
      },

      error: error => {
        console.error(
          'Failed to decline invitation:',
          error
        );

        this.invitationActionLoading.set(null);
      }
    });
  }
}