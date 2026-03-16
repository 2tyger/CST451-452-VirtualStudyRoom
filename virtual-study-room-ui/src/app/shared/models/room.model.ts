/*
declares frontend data models shared across services and components
*/
export interface Room {
  id: number;
  name: string;
  ownerId: number;
  memberCount: number;
  active: boolean;
  breakPhase: boolean;
  isRunning: boolean;
  elapsedSeconds: number;
  startTime?: string;
  createdAt: string;
}

export interface RoomMember {
  userId: number;
  displayName: string;
  role: string;
}

export interface RoomDetail {
  room: Room;
  members: RoomMember[];
}

