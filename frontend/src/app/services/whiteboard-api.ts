import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CreateShapeDto, CreateStrokeDto, UpdateShapeDto, UpdateStrokeDto } from './dto/dto';
import { Element, Shape, Stroke } from '../features/whiteboard/canvas/models';
@Injectable({
  providedIn: 'root',
})
export class WhiteboardApi {

  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  getBoardElements(boardId: string) {
    return this.http.get<Element[]>(
      `${this.baseUrl}/elements/board/${boardId}`
    );
  }

  createShape(boardId: string, shape: CreateShapeDto) {
    return this.http.post<Shape>(
      `${this.baseUrl}/elements/board/${boardId}/shape`,
      shape
    );
  }


  createStroke(boardId: string, stroke: CreateStrokeDto) {
    return this.http.post<Stroke>(
      `${this.baseUrl}/elements/board/${boardId}/stroke`,
      stroke
    );
  }

  updateShape(
    boardId: string,
    elementId: string,
    shape: UpdateShapeDto
  ) {
    return this.http.patch<Shape>(
      `${this.baseUrl}/elements/board/${boardId}/${elementId}/shape`,
      shape
    );

  }


  updateStroke(
    boardId: string,
    elementId: string,
    stroke: UpdateStrokeDto
  ) {
    return this.http.patch<Stroke>(
      `${this.baseUrl}/elements/board/${boardId}/${elementId}/stroke`,
      stroke
    );
  }


  deleteElement(boardId: string, elementId: string) {
    return this.http.delete(
      `${this.baseUrl}/elements/board/${boardId}/${elementId}`
    );
  }


  deleteAllElements(boardId: string) {
    return this.http.delete(
      `${this.baseUrl}/elements/board/${boardId}`
    );
  }


}
