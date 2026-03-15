import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomMessage } from '../../shared/models/message.model';
import { Room, RoomDetail } from '../../shared/models/room.model';

export interface TimerStateResponse {
  isRunning: boolean;
  elapsedSeconds: number;
  startTime?: string;
  phase: 'FOCUS' | 'BREAK';
  phaseDurationSeconds: number;
  remainingSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class RoomApiService {
  private readonly apiBase = 'http://localhost:8080/api/rooms';

  constructor(private http: HttpClient) {}

  listRooms(): Observable<Room[]> {
    return this.http.get<Room[]>(this.apiBase);
  }

  createRoom(name: string): Observable<Room> {
    return this.http.post<Room>(this.apiBase, { name });
  }

  joinRoom(roomId: number): Observable<Room> {
    return this.http.post<Room>(`${this.apiBase}/${roomId}/join`, {});
  }

  leaveRoom(roomId: number): Observable<Room> {
    return this.http.post<Room>(`${this.apiBase}/${roomId}/leave`, {});
  }

  deleteRoom(roomId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/${roomId}`);
  }

  listMessages(roomId: number, limit = 50): Observable<RoomMessage[]> {
    return this.http.get<RoomMessage[]>(`${this.apiBase}/${roomId}/messages`, {
      params: { limit: String(limit) }
    });
  }

  getRoom(roomId: number): Observable<RoomDetail> {
    return this.http.get<RoomDetail>(`${this.apiBase}/${roomId}`);
  }

  startTimer(roomId: number): Observable<TimerStateResponse> {
    return this.http.post<TimerStateResponse>(`${this.apiBase}/${roomId}/timer/start`, {});
  }

  pauseTimer(roomId: number): Observable<TimerStateResponse> {
    return this.http.post<TimerStateResponse>(`${this.apiBase}/${roomId}/timer/pause`, {});
  }

  resetTimer(roomId: number): Observable<TimerStateResponse> {
    return this.http.post<TimerStateResponse>(`${this.apiBase}/${roomId}/timer/reset`, {});
  }
}