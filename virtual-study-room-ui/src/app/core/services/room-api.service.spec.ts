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

  it('should create a room', () => {
    service.createRoom('Capstone Room').subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Capstone Room' });
    req.flush({});
  });

  it('should join a room', () => {
    service.joinRoom(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/join`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should leave a room', () => {
    service.leaveRoom(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/leave`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should delete a room', () => {
    service.deleteRoom(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should request room detail', () => {
    service.getRoom(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10`);
    expect(req.request.method).toBe('GET');
    req.flush({ room: {}, members: [] });
  });

  it('should request room messages with default limit', () => {
    service.listMessages(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/messages?limit=50`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should start pause and reset timer', () => {
    service.startTimer(10).subscribe();
    let req = httpMock.expectOne(`${environment.apiBase}/rooms/10/timer/start`);
    expect(req.request.method).toBe('POST');
    req.flush({ isRunning: true, elapsedSeconds: 0, phase: 'FOCUS', phaseDurationSeconds: 1500, remainingSeconds: 1500 });

    service.pauseTimer(10).subscribe();
    req = httpMock.expectOne(`${environment.apiBase}/rooms/10/timer/pause`);
    expect(req.request.method).toBe('POST');
    req.flush({ isRunning: false, elapsedSeconds: 10, phase: 'FOCUS', phaseDurationSeconds: 1500, remainingSeconds: 1490 });

    service.resetTimer(10).subscribe();
    req = httpMock.expectOne(`${environment.apiBase}/rooms/10/timer/reset`);
    expect(req.request.method).toBe('POST');
    req.flush({ isRunning: false, elapsedSeconds: 0, phase: 'FOCUS', phaseDurationSeconds: 1500, remainingSeconds: 1500 });
  });
});


