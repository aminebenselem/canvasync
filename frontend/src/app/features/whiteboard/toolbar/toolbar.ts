import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { WorkspaceApi } from '../../../services/workspace-api';
import { BoardMember, BoardPermission, Invitation } from '../canvas/models';
import { WhiteboardApi } from '../../../services/whiteboard-api';


export type ToolType =
  | 'select'
  | 'pen'
  | 'eraser'
  | 'rectangle'
  | 'ellipse'
  | 'line'
  | 'arrow'
  | 'text'
  | 'sticky'
  | 'pan';


interface Tool {
  id: ToolType;
  label: string;
  icon: string;
}


@Component({
  selector: 'app-toolbar',
  imports: [CommonModule, FormsModule],
  templateUrl: './toolbar.html',
  styleUrl: './toolbar.css',
})
export class Toolbar implements OnInit {

  // =========================
  // Existing outputs
  // =========================

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


  // =========================
  // Existing toolbar state
  // =========================

  activeTool = signal<ToolType>('select');
  activeColor = signal<string>('#1e1e1e');
  strokeWidth = signal<number>(3);
  fontSize = signal<number>(20);
  zoom = signal<number>(100);
  showColorPicker = signal(false);


  // =========================
  // Members state
  // =========================
   isOwner :boolean= false;
  canEdit:boolean = false;

  showMembersDialog = signal(false);

  members = signal<BoardMember[]>([]);

  membersLoading = signal(false);
  membersError = signal<string | null>(null);

  removingMemberId = signal<number | null>(null);
  updatingMemberId = signal<number | null>(null);


  
  // =========================
  // Invitation state
  // =========================

  showInviteDialog = signal(false);

  inviteEmail = '';
  invitePermission: BoardPermission = 'VIEWER';

  sendInvitationLoading = signal(false);
  sendInvitationError = signal<string | null>(null);

  boardId = '';


  // =========================
  // Tools
  // =========================

  tools: Tool[] = [
    {
      id: 'select',
      label: 'Select',
      icon: 'M4 4l7 16 2-7 7-2z'
    },
    {
      id: 'pen',
      label: 'Pen',
      icon: 'M15 5l4 4L7 21H3v-4z'
    },
    {
      id: 'eraser',
      label: 'Eraser',
      icon: 'M3 17l6 4h4l8-8-6-6-8 8z'
    },
    {
      id: 'rectangle',
      label: 'Rectangle',
      icon: 'M4 4h16v16H4z'
    },
    {
      id: 'ellipse',
      label: 'Ellipse',
      icon: 'M12 4a8 8 0 100 16 8 8 0 000-16z'
    },
    {
      id: 'line',
      label: 'Line',
      icon: 'M4 20L20 4'
    },
    {
      id: 'arrow',
      label: 'Arrow',
      icon: 'M4 20L20 4M20 4h-6M20 4v6'
    },
    {
      id: 'text',
      label: 'Text',
      icon: 'M5 4h14M12 4v16'
    },
    {
      id: 'pan',
      label: 'Pan',
      icon: 'M7 11V5a2 2 0 0 1 4 0v6M11 11V3a2 2 0 0 1 4 0v8M15 11V5a2 2 0 0 1 4 0v8M7 9a2 2 0 0 0-4 0v3c0 5 4 9 9 9h1a6 6 0 0 0 6-6'
    }
  ];


  palette = [
    '#1e1e1e',
    '#e03131',
    '#2f9e44',
    '#1971c2',
    '#f08c00',
    '#ffffff'
  ];


  fontSizes: number[] = [
    16,
    20,
    28,
    36,
    48,
    64
  ];


  constructor(
    private route: ActivatedRoute,
    private workspaceApi: WorkspaceApi,
    private whiteboardApi: WhiteboardApi
  ) {

    /*
     * Toolbar lives under the same /whiteboard/:boardId route,
     * so we can read the board ID directly here.
     */
    this.boardId =
      this.route.snapshot.paramMap.get('boardId') ?? '';

  }
  ngOnInit(): void {
this.isBoardOwner(this.boardId);
this.caneEditBoard(this.boardId);
  }


  // =========================
  // Existing toolbar methods
  // =========================

  selectTool(id: ToolType): void {
    this.activeTool.set(id);
    this.toolChange.emit(id);
  }


  selectColor(color: string): void {
    this.activeColor.set(color);
    this.colorChange.emit(color);
    this.showColorPicker.set(false);
  }


  setStrokeWidth(value: number): void {
    this.strokeWidth.set(value);
    this.strokeWidthChange.emit(value);
  }


  setFontSize(value: number): void {
    this.fontSize.set(value);
    this.fontSizeChange.emit(value);
  }


  zoomIn(): void {
    const z = Math.min(400, this.zoom() + 10);

    this.zoom.set(z);
    this.zoomChange.emit(z);
  }


  zoomOut(): void {
    const z = Math.max(10, this.zoom() - 10);

    this.zoom.set(z);
    this.zoomChange.emit(z);
  }


  resetZoom(): void {
    this.zoom.set(100);
    this.zoomChange.emit(100);
  }


  // =========================
  // Invitation
  // =========================

  openInviteDialog(): void {

    this.inviteEmail = '';
    this.invitePermission = 'VIEWER';

    this.sendInvitationError.set(null);

    this.showInviteDialog.set(true);
  }


  closeInviteDialog(): void {

    if (this.sendInvitationLoading()) {
      return;
    }

    this.showInviteDialog.set(false);

    this.inviteEmail = '';

    this.sendInvitationError.set(null);
  }


  sendInvitation(): void {

    const email = this.inviteEmail.trim();

    // -------------------------
    // Validate email
    // -------------------------

    if (!email) {

      this.sendInvitationError.set(
        'Email is required.'
      );

      return;
    }


    // -------------------------
    // Validate board ID
    // -------------------------

    if (!this.boardId) {

      this.sendInvitationError.set(
        'Board ID is missing.'
      );

      return;
    }


    // -------------------------
    // Start request
    // -------------------------

    this.sendInvitationLoading.set(true);

    this.sendInvitationError.set(null);


    // -------------------------
    // API call
    // -------------------------

    this.workspaceApi.sendInvitation(
      this.boardId,
      email,
      this.invitePermission
    ).subscribe({

      next: (_invitation: Invitation) => {

        this.sendInvitationLoading.set(false);

        this.closeInviteDialog();
      },


      error: (error) => {

        console.error(
          'Failed to send invitation:',
          error
        );

        this.sendInvitationLoading.set(false);


        if (error.status === 404) {

          this.sendInvitationError.set(
            'No user was found with this email.'
          );

        }

        else if (error.status === 409) {

          this.sendInvitationError.set(
            'This user already has a pending invitation.'
          );

        }

        else if (error.status === 403) {

          this.sendInvitationError.set(
            'You do not have permission to invite members.'
          );

        }

        else {

          this.sendInvitationError.set(
            'Failed to send invitation. Please try again.'
          );

        }

      }

    });
  }

  openMembersDialog(): void {

  if (!this.boardId) {
    this.membersError.set('Board ID is missing.');
    return;
  }

  this.showMembersDialog.set(true);
  this.membersLoading.set(true);
  this.membersError.set(null);

  this.workspaceApi.getBoardMembers(this.boardId).subscribe({

    next: (members) => {
      this.members.set(members);
      this.membersLoading.set(false);
      console.log('Members loaded:', members);
    },

    error: (error) => {
      console.error('Failed to load members:', error);

      this.membersLoading.set(false);
      this.membersError.set(
        'Failed to load board members.'
      );
    }

  });
}

closeMembersDialog(): void {

  if (
    this.membersLoading() ||
    this.removingMemberId() !== null ||
    this.updatingMemberId() !== null
  ) {
    return;
  }

  this.showMembersDialog.set(false);
}
removeMember(member: BoardMember): void {

  if (!this.boardId) {
    return;
  }

  if (!confirm(`Remove ${member.email} from this board?`)) {
    return;
  }

  this.removingMemberId.set(member.userId);

  this.workspaceApi
    .removeBoardMember(this.boardId, member.userId)
    .subscribe({

      next: () => {

        this.members.update(
          members =>
            members.filter(m => m.userId !== member.userId)
        );

        this.removingMemberId.set(null);
      },

      error: (error) => {

        console.error(
          'Failed to remove member:',
          error
        );

        this.removingMemberId.set(null);

        this.membersError.set(
          error.status === 403
            ? 'You do not have permission to remove members.'
            : 'Failed to remove member.'
        );
      }

    });
}
updatePermission(
  member: BoardMember,
  permission: BoardPermission
): void {

  if (!this.boardId) {
    return;
  }

  this.updatingMemberId.set(member.userId);

  this.workspaceApi
    .updateMemberPermission(
      this.boardId,
      member.userId,
      permission
    )
    .subscribe({

      next: () => {

        this.members.update(members =>
          members.map(m =>
            m.userId === member.userId
              ? { ...m, permission }
              : m
          )
        );

        this.updatingMemberId.set(null);
      },

      error: (error) => {

        console.error(
          'Failed to update member permission:',
          error
        );

        this.updatingMemberId.set(null);

        this.membersError.set(
          error.status === 403
            ? 'You do not have permission to change member permissions.'
            : 'Failed to update member permission.'
        );
      }

    });
}
  isBoardOwner(boardId: string) {
    this.whiteboardApi.isOwner(boardId).subscribe({
      next: (isOwner: boolean) => {
        this.isOwner = isOwner;
      },
      error: (error: unknown) => {
        console.error('Failed to check ownership:', error);
        return false;
      }
    })
  }
  caneEditBoard(boardId: string) {
    this.whiteboardApi.canEdit(boardId).subscribe({
      next: (canEdit: boolean) => {
        this.canEdit = canEdit;
      },
      error: (error: unknown) => {
        console.error('Failed to check edit permission:', error);
        return false;
      }
    })
  }
}