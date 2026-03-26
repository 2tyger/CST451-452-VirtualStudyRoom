/*
contains websocket service tests for parsing and chat transport behavior
*/
import { TestBed } from '@angular/core/testing';
import { WsService } from './ws.service';
import { AuthService } from './auth.service';

describe('WsService', () => {
  let service: WsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WsService,
        {
          provide: AuthService,
          useValue: {
            getToken: () => 'token'
          }
        }
      ]
    });

    service = TestBed.inject(WsService);
  });

  it('should parse timer update event payload', () => {
    const raw = JSON.stringify({
      type: 'timer_update',
      roomId: 10,
      timestamp: '2025-01-01T00:00:00Z',
      payload: {
        isRunning: true,
        elapsedSeconds: 10,
        phase: 'BREAK',
        phaseDurationSeconds: 300,
        remainingSeconds: 290
      }
    });

    const event = (service as unknown as { parseRoomEvent: (body: string, roomId: number) => { type: string; payload: { phase?: string; remainingSeconds?: number } } }).parseRoomEvent(raw, 10);

    expect(event.type).toBe('timer_update');
    expect(event.payload.phase).toBe('BREAK');
    expect(event.payload.remainingSeconds).toBe(290);
  });

  it('should parse task update event payload', () => {
    const raw = JSON.stringify({
      type: 'task_update',
      roomId: 10,
      timestamp: '2025-01-01T00:00:00Z',
      payload: {
        action: 'added',
        task: {
          id: 3,
          roomId: 10,
          title: 'Write tests',
          done: false,
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z'
        }
      }
    });

    const event = (service as unknown as { parseRoomEvent: (body: string, roomId: number) => { type: string; payload: { action?: string; task?: { title?: string } } } }).parseRoomEvent(raw, 10);

    expect(event.type).toBe('task_update');
    expect(event.payload.action).toBe('added');
    expect(event.payload.task?.title).toBe('Write tests');
  });

  it('should parse chat update event payload', () => {
    const raw = JSON.stringify({
      type: 'chat_message',
      roomId: 10,
      timestamp: '2025-01-01T00:00:00Z',
      payload: {
        sender: 'Ty',
        body: 'hello'
      }
    });

    const event = (service as unknown as { parseRoomEvent: (body: string, roomId: number) => { type: string; payload: { sender?: string; body?: string } } }).parseRoomEvent(raw, 10);

    expect(event.type).toBe('chat_message');
    expect(event.payload.sender).toBe('Ty');
    expect(event.payload.body).toBe('hello');
  });

  it('should fallback to raw event when json parsing fails', () => {
    const event = (service as unknown as { parseRoomEvent: (body: string, roomId: number) => { type: string; payload: { message?: string } } }).parseRoomEvent('invalid-json', 10);

    expect(event.type).toBe('raw');
    expect(event.payload.message).toBe('invalid-json');
  });

  it('should map stomp rate limit errors to friendly message', () => {
    const frame = {
      headers: { message: 'rate limit exceeded' },
      body: ''
    };

    const message = (service as unknown as { mapStompError: (frame: { headers: { message: string }; body: string }) => string }).mapStompError(frame);
    expect(message).toContain('sending messages too quickly');
  });

  it('should send chat payload using active client', () => {
    const publish = jasmine.createSpy('publish');
    (service as unknown as { client: { publish: (payload: { destination: string; body: string }) => void } }).client = { publish };

    service.sendChat(10, 'hello');

    expect(publish).toHaveBeenCalledWith({
      destination: '/app/rooms/10/chat.send',
      body: JSON.stringify({ body: 'hello' })
    });
  });

  it('should deactivate client on disconnect', () => {
    const deactivate = jasmine.createSpy('deactivate');
    (service as unknown as { client: { deactivate: () => void } }).client = { deactivate };

    service.disconnect();

    expect(deactivate).toHaveBeenCalled();
  });
});
