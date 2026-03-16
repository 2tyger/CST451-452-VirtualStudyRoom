/*
declares frontend data models shared across services and components
*/
import { Task } from './task.model';

export type SupportedRoomEventType = 'timer_update' | 'task_update' | 'chat_message';

interface RoomEventBase<TType extends string, TPayload> {
  type: TType;
  roomId: number;
  timestamp: string;
  payload: TPayload;
}

export interface TimerUpdatePayload {
  isRunning: boolean;
  elapsedSeconds: number;
  startTime?: string;
  phase?: 'FOCUS' | 'BREAK';
  phaseDurationSeconds?: number;
  remainingSeconds?: number;
}

export interface TaskUpdatePayload {
  action: string;
  task: Task;
}

export interface ChatMessagePayload {
  sender: string;
  body: string;
}

export type TimerUpdateEvent = RoomEventBase<'timer_update', TimerUpdatePayload>;
export type TaskUpdateEvent = RoomEventBase<'task_update', TaskUpdatePayload>;
export type ChatMessageEvent = RoomEventBase<'chat_message', ChatMessagePayload>;

export type SupportedRoomEvent = TimerUpdateEvent | TaskUpdateEvent | ChatMessageEvent;

export type UnknownRoomEvent = RoomEventBase<'raw', Record<string, unknown>>;

export type RoomEvent = SupportedRoomEvent | UnknownRoomEvent;

