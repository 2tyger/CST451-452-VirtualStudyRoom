/*
wraps frontend api or websocket communication for feature components
*/
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProfileResponse, UpdateProfileRequest } from '../../shared/models/profile.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly apiBase = `${environment.apiBase}/profile`;

  constructor(private http: HttpClient) {}

  getMyProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.apiBase}/me`);
  }

  updateMyProfile(payload: UpdateProfileRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${this.apiBase}/me`, payload);
  }
}

