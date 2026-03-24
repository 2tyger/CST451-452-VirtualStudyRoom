/*
wraps frontend api or websocket communication for feature components
*/
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomMessage } from '../../shared/models/message.model';
import { Room, RoomDetail } from '../../shared/models/room.model';
import { environment } from '../../../environments/environment';

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
  private readonly apiBase = `${environment.apiBase}/rooms`;

  constructor(private http: HttpClient) {}

  // loads rooms for the current authenticated user
  listRooms(): Observable<Room[]> {
    return this.http.get<Room[]>(this.apiBase);
  }

  // creates a room with the supplied display name
  createRoom(name: string): Observable<Room> {
    return this.http.post<Room>(this.apiBase, { name });
  }

  // joins a room by room id
  joinRoom(roomId: number): Observable<Room> {
    return this.http.post<Room>(`${this.apiBase}/${roomId}/join`, {});
  }

  // leaves a room for the current user
  leaveRoom(roomId: number): Observable<Room> {
    return this.http.post<Room>(`${this.apiBase}/${roomId}/leave`, {});
  }

  // deletes a room owned by the current user
  deleteRoom(roomId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/${roomId}`);
  }

  // loads persisted room message history
  listMessages(roomId: number, limit = 50): Observable<RoomMessage[]> {
    return this.http.get<RoomMessage[]>(`${this.apiBase}/${roomId}/messages`, {
      params: { limit: String(limit) }
    });
  }

  // loads room details and participant list
  getRoom(roomId: number): Observable<RoomDetail> {
    return this.http.get<RoomDetail>(`${this.apiBase}/${roomId}`);
  }

  // starts the room timer
  startTimer(roomId: number): Observable<TimerStateResponse> {
    return this.http.post<TimerStateResponse>(`${this.apiBase}/${roomId}/timer/start`, {});
  }

  // pauses the room timer
  pauseTimer(roomId: number): Observable<TimerStateResponse> {
    return this.http.post<TimerStateResponse>(`${this.apiBase}/${roomId}/timer/pause`, {});
  }

  // resets the room timer to default state
  resetTimer(roomId: number): Observable<TimerStateResponse> {
    return this.http.post<TimerStateResponse>(`${this.apiBase}/${roomId}/timer/reset`, {});
  }
}

