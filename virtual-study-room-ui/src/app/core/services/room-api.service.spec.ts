/*
contains frontend service smoke tests for api request wiring
*/
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RoomApiService } from './room-api.service';
import { environment } from '../../../environments/environment';

describe('RoomApiService', () => {
  let service: RoomApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(RoomApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should request room list', () => {
    service.listRooms().subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});


