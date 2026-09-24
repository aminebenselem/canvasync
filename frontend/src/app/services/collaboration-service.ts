import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root',
})
export class CollaborationService {

  private client: Client;
  private currentBoardId?: string;

  constructor() {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/api/ws',

      onConnect: () => {
        console.log('WebSocket connected');

        if (this.currentBoardId) {
          this.subscribeToBoard(this.currentBoardId);
        }
      },
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('access_token')}`
      },

      onDisconnect: () => {
        console.log('WebSocket disconnected');
      },

      onStompError: error => {
        console.error('STOMP error:', error);
      }
    });
  }

  connectToBoard(boardId: string): void {
    this.currentBoardId = boardId;

    this.client.activate();
  }

  subscribeToBoard(boardId: string): void {
    this.client.subscribe(
      `/topic/boards/${boardId}`,
      message => {
        console.log(
          'Board event:',
          JSON.parse(message.body)
        );
      }
    );
  }

  sendTest(): void {
    this.client.publish({
      destination: `/app/boards/${this.currentBoardId}/test`,
      body: JSON.stringify({
        message: 'Hello from Angular'
      })
    });
  }
}