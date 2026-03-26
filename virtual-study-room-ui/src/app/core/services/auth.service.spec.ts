/*
contains frontend service tests for auth session behavior
*/
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should persist session on login', () => {
    const token = makeToken(3600);
    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({ token, userId: 10, email: 'user@example.com', displayName: 'Ty' });

    expect(localStorage.getItem('vsr_token')).toBe(token);
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.getCurrentUserId()).toBe(10);
  });

  it('should persist session on register', () => {
    const token = makeToken(3600);
    service.register({ displayName: 'Ty', email: 'user@example.com', password: 'password123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush({ token, userId: 11, email: 'user@example.com', displayName: 'Ty' });

    expect(localStorage.getItem('vsr_token')).toBe(token);
    expect(service.getCurrentUserId()).toBe(11);
  });

  it('should clear session on logout', () => {
    localStorage.setItem('vsr_token', makeToken(3600));
    localStorage.setItem('vsr_user', JSON.stringify({ userId: 5 }));

    service.logout();

    expect(localStorage.getItem('vsr_token')).toBeNull();
    expect(localStorage.getItem('vsr_user')).toBeNull();
  });

  it('should clear invalid token from getToken', () => {
    localStorage.setItem('vsr_token', makeToken(-10));
    localStorage.setItem('vsr_user', JSON.stringify({ userId: 5 }));

    const token = service.getToken();

    expect(token).toBeNull();
    expect(localStorage.getItem('vsr_token')).toBeNull();
  });

  it('should return null user id for malformed storage payload', () => {
    localStorage.setItem('vsr_user', 'not-json');
    expect(service.getCurrentUserId()).toBeNull();
  });

  function makeToken(expiresInSecondsFromNow: number): string {
    const payload = {
      exp: Math.floor(Date.now() / 1000) + expiresInSecondsFromNow
    };
    const encoded = btoa(JSON.stringify(payload));
    return `header.${encoded}.signature`;
  }
});
