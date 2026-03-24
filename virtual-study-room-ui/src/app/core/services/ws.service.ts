/*
wraps frontend api or websocket communication for feature components
*/
import { Injectable } from '@angular/core';
import { Client, IFrame, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { Task } from '../../shared/models/task.model';
import { RoomEvent, SupportedRoomEventType } from '../../shared/models/ws.model';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WsService {
  private client?: Client;
  private readonly eventsSubject = new Subject<RoomEvent>();
  private readonly errorsSubject = new Subject<string>();
  readonly events$ = this.eventsSubject.asObservable();
  readonly errors$ = this.errorsSubject.asObservable();
  private readonly brokerUrl = environment.wsBase;

  constructor(private authService: AuthService) {}

  // opens websocket connection and subscribes to room and user error channels
  connect(roomId: number): void {
    if (this.client?.connected) {
      return;
    }

    this.client = new Client({
      brokerURL: this.brokerUrl,
      connectHeaders: this.getConnectHeaders(),
      reconnectDelay: 5000,
      debug: () => {}
    });

    this.client.onConnect = () => {
      this.client?.subscribe(`/topic/rooms/${roomId}`, (message: IMessage) => {
        this.eventsSubject.next(this.parseRoomEvent(message.body, roomId));
      });

      this.client?.subscribe('/user/queue/errors', (message: IMessage) => {
        this.errorsSubject.next(this.parseWsErrorMessage(message.body));
      });
    };

    this.client.onStompError = (frame: IFrame) => {
      this.errorsSubject.next(this.mapStompError(frame));
    };

    this.client.onWebSocketError = () => {
      this.errorsSubject.next('Unable to send chat right now. Please try again.');
    };

    this.client.activate();
  }

  // publishes a chat command to the room websocket endpoint
  sendChat(roomId: number, body: string): void {
    this.client?.publish({
      destination: `/app/rooms/${roomId}/chat.send`,
      body: JSON.stringify({ body })
    });
  }

  // closes websocket connection when room view is torn down
  disconnect(): void {
    this.client?.deactivate();
    this.client = undefined;
  }

  private getConnectHeaders(): Record<string, string> {
    const token = this.authService.getToken();
    if (!token) {
      return {};
    }
    return {
      Authorization: `Bearer ${token}`
    };
  }

  // parses incoming websocket payloads into typed room events
  private parseRoomEvent(rawBody: string, roomId: number): RoomEvent {
    try {
      const parsed = JSON.parse(rawBody) as Record<string, unknown>;
      return this.toTypedEvent(parsed, roomId);
    } catch {
      return {
        type: 'raw',
        roomId,
        timestamp: new Date().toISOString(),
        payload: {
          message: rawBody
        }
      };
    }
  }

  // maps websocket event envelope values to typed frontend event models
  private toTypedEvent(parsed: Record<string, unknown>, fallbackRoomId: number): RoomEvent {
    const type = String(parsed['type'] ?? 'raw');
    const roomId = Number(parsed['roomId'] ?? fallbackRoomId);
    const timestamp = this.asTimestamp(parsed['timestamp']);
    const payload = this.asRecord(parsed['payload']);

    if (this.isSupportedEventType(type)) {
      if (type === 'timer_update') {
        const phase = payload['phase'] === 'BREAK' ? 'BREAK' : 'FOCUS';
        return {
          type,
          roomId,
          timestamp,
          payload: {
            isRunning: Boolean(payload['isRunning']),
            elapsedSeconds: Number(payload['elapsedSeconds'] ?? 0),
            startTime: typeof payload['startTime'] === 'string' ? payload['startTime'] : undefined,
            phase,
            phaseDurationSeconds: Number(payload['phaseDurationSeconds'] ?? (phase === 'BREAK' ? 300 : 1500)),
            remainingSeconds: Number(payload['remainingSeconds'] ?? 0)
          }
        };
      }

      if (type === 'task_update') {
        return {
          type,
          roomId,
          timestamp,
          payload: {
            action: String(payload['action'] ?? 'updated'),
            task: this.asTask(payload['task'])
          }
        };
      }

      return {
        type,
        roomId,
        timestamp,
        payload: {
          sender: String(payload['sender'] ?? 'user'),
          body: String(payload['body'] ?? '')
        }
      };
    }

    return {
      type: 'raw',
      roomId,
      timestamp,
      payload: {
        ...payload,
        originalType: type
      }
    };
  }

  private isSupportedEventType(type: string): type is SupportedRoomEventType {
    return type === 'timer_update' || type === 'task_update' || type === 'chat_message';
  }

  private asTimestamp(value: unknown): string {
    return typeof value === 'string' ? value : new Date().toISOString();
  }

  private asRecord(value: unknown): Record<string, unknown> {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return value as Record<string, unknown>;
    }
    return {};
  }

  private asTask(value: unknown): Task {
    const task = this.asRecord(value);
    return {
      id: Number(task['id'] ?? 0),
      roomId: Number(task['roomId'] ?? 0),
      title: String(task['title'] ?? ''),
      description: typeof task['description'] === 'string' ? task['description'] : undefined,
      assigneeId: typeof task['assigneeId'] === 'number' ? task['assigneeId'] : undefined,
      done: Boolean(task['done']),
      createdAt: String(task['createdAt'] ?? new Date().toISOString()),
      updatedAt: String(task['updatedAt'] ?? new Date().toISOString())
    };
  }

  // maps stomp error frames to user friendly chat error messages
  private mapStompError(frame: IFrame): string {
    const headerMessage = frame.headers['message'] ?? '';
    const body = frame.body ?? '';
    const combined = `${headerMessage} ${body}`.toLowerCase();

    if (combined.includes('too many') || combined.includes('rate limit')) {
      return 'You are sending messages too quickly. Please wait a moment and try again.';
    }

    return 'Unable to send chat right now. Please try again.';
  }

  // parses user queue websocket error payloads
  private parseWsErrorMessage(rawBody: string): string {
    try {
      const parsed = JSON.parse(rawBody) as Record<string, unknown>;
      const code = String(parsed['code'] ?? '');
      const message = String(parsed['message'] ?? '');

      if (code === 'RATE_LIMITED' && message) {
        return message;
      }

      return message || 'Unable to send chat right now. Please try again.';
    } catch {
      return rawBody || 'Unable to send chat right now. Please try again.';
    }
  }
}

