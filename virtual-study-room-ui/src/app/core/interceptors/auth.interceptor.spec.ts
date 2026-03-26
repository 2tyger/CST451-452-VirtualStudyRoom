/*
contains interceptor tests for auth header behavior
*/
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  it('should append authorization header when token exists', done => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['getToken']);
    authService.getToken.and.returnValue('token-123');

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authService }]
    });

    const req = new HttpRequest('GET', '/api/rooms');
    TestBed.runInInjectionContext(() => {
      authInterceptor(req, nextReq => {
        expect(nextReq.headers.get('Authorization')).toBe('Bearer token-123');
        return of(new HttpResponse({ status: 200 }));
      }).subscribe(() => done());
    });
  });

  it('should leave headers unchanged when no token exists', done => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['getToken']);
    authService.getToken.and.returnValue(null);

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authService }]
    });

    const req = new HttpRequest('GET', '/api/rooms');
    TestBed.runInInjectionContext(() => {
      authInterceptor(req, nextReq => {
        expect(nextReq.headers.has('Authorization')).toBeFalse();
        return of(new HttpResponse({ status: 200 }));
      }).subscribe(() => done());
    });
  });
});
