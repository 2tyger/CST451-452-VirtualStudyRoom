import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RoomApiService } from './room-api.service';

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

    const req = httpMock.expectOne('http://localhost:8080/api/rooms');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
