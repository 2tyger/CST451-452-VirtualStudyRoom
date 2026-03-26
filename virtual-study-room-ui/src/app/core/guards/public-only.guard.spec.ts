/*
contains route guard tests for public only pages
*/
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { publicOnlyGuard } from './public-only.guard';
import { AuthService } from '../services/auth.service';

describe('publicOnlyGuard', () => {
  it('should redirect authenticated users to dashboard', () => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);
    authService.isAuthenticated.and.returnValue(true);
    const tree = {} as UrlTree;
    const router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    router.createUrlTree.and.returnValue(tree);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });

    const result = TestBed.runInInjectionContext(() => publicOnlyGuard({} as never, {} as never));
    expect(router.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
    expect(result).toBe(tree);
  });

  it('should allow unauthenticated users', () => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);
    authService.isAuthenticated.and.returnValue(false);
    const router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });

    const result = TestBed.runInInjectionContext(() => publicOnlyGuard({} as never, {} as never));
    expect(result).toBeTrue();
  });
});
