/*
declares frontend data models shared across services and components
*/
export interface RoomMessage {
  id: number;
  roomId: number;
  userId: number;
  sender: string;
  body: string;
  createdAt: string;
}

