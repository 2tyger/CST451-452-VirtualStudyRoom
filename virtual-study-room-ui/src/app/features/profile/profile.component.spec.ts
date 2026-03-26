/*
contains component tests for profile behavior
*/
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { ProfileApiService } from '../../core/services/profile-api.service';

describe('ProfileComponent', () => {
  let profileApi: jasmine.SpyObj<ProfileApiService>;

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

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [{ provide: ProfileApiService, useValue: profileApi }]
    }).compileComponents();
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
    expect(component.success).toBe('Profile updated successfully.');
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
