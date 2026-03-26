/*
contains frontend service tests for profile api wiring
*/
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProfileApiService } from './profile-api.service';
import { environment } from '../../../environments/environment';

describe('ProfileApiService', () => {
  let service: ProfileApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(ProfileApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request current profile', () => {
    service.getMyProfile().subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/profile/me`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should update current profile', () => {
    const payload = {
      displayName: 'Ty',
      email: 'ty@example.com',
      bio: 'bio',
      newPassword: 'password123'
    };

    service.updateMyProfile(payload).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/profile/me`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });
});
