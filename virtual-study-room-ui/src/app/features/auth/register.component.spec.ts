/*
contains component tests for registration behavior
*/
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../core/services/auth.service';

describe('RegisterComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([])
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should register and navigate to dashboard', () => {
    authService.register.and.returnValue(of({ token: 'token', userId: 1, email: 'user@example.com', displayName: 'Ty' }));

    const fixture = TestBed.createComponent(RegisterComponent);
    const component = fixture.componentInstance;
    component.form.setValue({ displayName: 'Ty', email: 'user@example.com', password: 'password123' });

    component.submit();

    expect(authService.register).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading).toBeFalse();
  });

  it('should not submit invalid form', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    const component = fixture.componentInstance;
    component.form.setValue({ displayName: 'T', email: 'bad-email', password: 'short' });

    component.submit();

    expect(authService.register).not.toHaveBeenCalled();
  });

  it('should show error when registration fails', () => {
    authService.register.and.returnValue(throwError(() => new Error('failed')));

    const fixture = TestBed.createComponent(RegisterComponent);
    const component = fixture.componentInstance;
    component.form.setValue({ displayName: 'Ty', email: 'user@example.com', password: 'password123' });

    component.submit();

    expect(component.error).toBe('Registration failed.');
    expect(component.loading).toBeFalse();
  });
});
