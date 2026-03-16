/*
wraps frontend api or websocket communication for feature components
*/
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProfileResponse, UpdateProfileRequest } from '../../shared/models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly apiBase = 'http://localhost:8080/api/profile';

  constructor(private http: HttpClient) {}

  getMyProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.apiBase}/me`);
  }

  updateMyProfile(payload: UpdateProfileRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${this.apiBase}/me`, payload);
  }
}

