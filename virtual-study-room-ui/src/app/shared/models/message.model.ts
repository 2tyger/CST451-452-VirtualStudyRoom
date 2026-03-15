export interface RoomMessage {
  id: number;
  roomId: number;
  userId: number;
  sender: string;
  body: string;
  createdAt: string;
}