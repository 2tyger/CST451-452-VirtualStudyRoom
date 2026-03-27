/*
wraps frontend api or websocket communication for feature components
*/
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../../shared/models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiBase = `${environment.apiBase}/auth`;
  private readonly tokenKey = 'vsr_token';
  private readonly userKey = 'vsr_user';

  private readonly authStateSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  readonly authState$ = this.authStateSubject.asObservable();

  constructor(private http: HttpClient) {}

  // sends login credentials and persists the returned auth session
  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBase}/login`, payload).pipe(
      tap(response => this.persistSession(response))
    );
  }

  // sends registration data and persists the returned auth session
  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBase}/register`, payload).pipe(
      tap(response => this.persistSession(response))
    );
  }

  // clears local auth session state
  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.authStateSubject.next(false);
  }

  // returns a valid token or clears invalid session state
  getToken(): string | null {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      return null;
    }

    if (!this.isTokenValid(token)) {
      this.logout();
      return null;
    }

    return token;
  }

  isAuthenticated(): boolean {
    return this.hasValidToken();
  }

  getCurrentUserId(): number | null {
    try {
      const raw = localStorage.getItem(this.userKey);
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw) as Partial<AuthResponse>;
      const id = Number(parsed.userId);
      return Number.isFinite(id) ? id : null;
    } catch {
      return null;
    }
  }

  // updates cached user identity fields after profile edits
  syncStoredIdentity(email: string, displayName: string): void {
    try {
      const raw = localStorage.getItem(this.userKey);
      if (!raw) {
        return;
      }

      const parsed = JSON.parse(raw) as Partial<AuthResponse>;
      const nextUser: AuthResponse = {
        token: typeof parsed.token === 'string' ? parsed.token : localStorage.getItem(this.tokenKey) ?? '',
        userId: Number(parsed.userId ?? 0),
        email,
        displayName
      };
      localStorage.setItem(this.userKey, JSON.stringify(nextUser));
    } catch {
      // keep existing session if local user cache cannot be parsed
    }
  }

  private persistSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.userKey, JSON.stringify(response));
    this.authStateSubject.next(true);
  }

  private hasValidToken(): boolean {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      return false;
    }
    return this.isTokenValid(token);
  }

  private isTokenValid(token: string): boolean {
    try {
      const payload = this.decodeJwtPayload(token);
      const exp = typeof payload.exp === 'number' ? payload.exp : Number(payload.exp);
      if (!exp || Number.isNaN(exp)) {
        return false;
      }
      return exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private decodeJwtPayload(token: string): { exp?: number | string } {
    const parts = token.split('.');
    if (parts.length < 2) {
      throw new Error('Invalid JWT');
    }

    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const normalized = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const json = atob(normalized);
    return JSON.parse(json) as { exp?: number | string };
  }
}

