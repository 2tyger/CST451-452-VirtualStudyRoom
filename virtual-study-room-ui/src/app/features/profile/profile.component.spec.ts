/*
contains component tests for profile behavior
*/
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { ProfileComponent } from './profile.component';
import { ProfileApiService } from '../../core/services/profile-api.service';
import { AuthService } from '../../core/services/auth.service';

describe('ProfileComponent', () => {
  let profileApi: jasmine.SpyObj<ProfileApiService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    profileApi = jasmine.createSpyObj<ProfileApiService>('ProfileApiService', ['getMyProfile', 'updateMyProfile']);
    profileApi.getMyProfile.and.returnValue(of({
      userId: 1,
      email: 'ty@example.com',
      displayName: 'Ty',
      bio: 'bio',
      createdAt: '2025-01-01T00:00:00Z'
    }));
    profileApi.updateMyProfile.and.returnValue(of({
      userId: 1,
      email: 'ty@example.com',
      displayName: 'Ty',
      bio: 'bio',
      createdAt: '2025-01-01T00:00:00Z'
    }));

    authService = jasmine.createSpyObj<AuthService>('AuthService', ['logout', 'syncStoredIdentity']);

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        { provide: ProfileApiService, useValue: profileApi },
        { provide: AuthService, useValue: authService },
        provideRouter([])
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should load profile on init', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.form.get('displayName')?.value).toBe('Ty');
    expect(profileApi.getMyProfile).toHaveBeenCalled();
  });

  it('should submit profile update with trimmed bio', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.setValue({
      displayName: 'Ty',
      email: 'ty@example.com',
      bio: '  bio value  ',
      newPassword: ''
    });

    component.submit();

    expect(profileApi.updateMyProfile).toHaveBeenCalledWith({
      displayName: 'Ty',
      email: 'ty@example.com',
      bio: 'bio value'
    });
    expect(authService.syncStoredIdentity).toHaveBeenCalledWith('ty@example.com', 'Ty');
    expect(component.success).toBe('Profile updated successfully.');
  });

  it('should force relogin when password changes', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.setValue({
      displayName: 'Ty',
      email: 'ty@example.com',
      bio: 'bio',
      newPassword: 'password123'
    });

    component.submit();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should force relogin when email changes', () => {
    profileApi.updateMyProfile.and.returnValue(of({
      userId: 1,
      email: 'new@example.com',
      displayName: 'Ty',
      bio: 'bio',
      createdAt: '2025-01-01T00:00:00Z'
    }));

    const fixture = TestBed.createComponent(ProfileComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.setValue({
      displayName: 'Ty',
      email: 'new@example.com',
      bio: 'bio',
      newPassword: ''
    });

    component.submit();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return fallback avatar initial when name missing', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({ displayName: '' });
    expect(component.avatarInitial).toBe('U');
  });

  it('should show load error when profile request fails', () => {
    profileApi.getMyProfile.and.returnValue(throwError(() => new Error('failed')));

    const fixture = TestBed.createComponent(ProfileComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Failed to load profile.');
  });
});
