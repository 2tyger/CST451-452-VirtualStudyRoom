/*
contains component tests for login behavior
*/
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';

describe('LoginComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([])
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should login and navigate to dashboard', () => {
    authService.login.and.returnValue(of({ token: 'token', userId: 1, email: 'user@example.com', displayName: 'Ty' }));

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    component.form.setValue({ email: 'user@example.com', password: 'password123' });

    component.submit();

    expect(authService.login).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading).toBeFalse();
  });

  it('should not submit invalid form', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    component.form.setValue({ email: '', password: '' });

    component.submit();

    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should show error when login fails', () => {
    authService.login.and.returnValue(throwError(() => new Error('bad credentials')));

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    component.form.setValue({ email: 'user@example.com', password: 'wrong' });

    component.submit();

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Login failed. Check credentials.');
  });
});
