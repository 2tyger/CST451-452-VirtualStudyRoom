import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task } from '../../shared/models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskApiService {
  private readonly apiBase = 'http://localhost:8080/api/rooms';

  constructor(private http: HttpClient) {}

  listTasks(roomId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiBase}/${roomId}/tasks`);
  }

  createTask(roomId: number, title: string, description = ''): Observable<Task> {
    return this.http.post<Task>(`${this.apiBase}/${roomId}/tasks`, { title, description });
  }

  updateTask(roomId: number, taskId: number, patch: Partial<Pick<Task, 'title' | 'description' | 'done'>>): Observable<Task> {
    return this.http.patch<Task>(`${this.apiBase}/${roomId}/tasks/${taskId}`, patch);
  }

  deleteTask(roomId: number, taskId: number): Observable<Task> {
    return this.http.delete<Task>(`${this.apiBase}/${roomId}/tasks/${taskId}`);
  }
}