export interface Task {
  id: number;
  roomId: number;
  title: string;
  description?: string;
  assigneeId?: number;
  done: boolean;
  createdAt: string;
  updatedAt: string;
}