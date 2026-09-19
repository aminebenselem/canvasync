import { ElementType, Point, ShapeType } from "../../features/whiteboard/canvas/models"

export interface UpdateShapeDto{
 color: string,
 width:number,
 startPoint: Point,
 endPoint: Point,
 text: string
}

export interface CreateShapeDto{
 type:ElementType,
 shapeType:ShapeType,
 color: string,
 width:number,
 startPoint: Point,
 endPoint: Point,
 text: string
}
export interface UpdateStrokeDto{
 color: string,
 width:number,
 points: Point[],
}
export interface CreateStrokeDto{
 color: string,
 width:number,
 points: Point[],
}