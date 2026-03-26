/*
renders the room dashboard and handles create join and navigation actions
*/
import { of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { RoomApiService } from '../../core/services/room-api.service';
import { Room } from '../../shared/models/room.model';

describe('DashboardComponent', () => {
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', ['listRooms', 'createRoom', 'joinRoom']);
    roomApi.listRooms.and.returnValue(of([]));
    roomApi.createRoom.and.returnValue(of({} as Room));
    roomApi.joinRoom.and.returnValue(of({ id: 1 } as Room));

    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: RoomApiService, useValue: roomApi },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load rooms and sort newest first', () => {
    const older = {
      id: 1,
      createdAt: '2025-01-01T00:00:00Z'
    } as Room;
    const newer = {
      id: 2,
      createdAt: '2025-02-01T00:00:00Z'
    } as Room;
    roomApi.listRooms.and.returnValue(of([older, newer]));

    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.rooms.map(room => room.id)).toEqual([2, 1]);
  });

  it('should validate join room id input before api call', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.joinRoomCodeInput = '0';
    component.joinRoom();

    expect(component.joinError).toBe('Enter a valid room ID.');
    expect(roomApi.joinRoom).not.toHaveBeenCalled();
  });

  it('should navigate to joined room on successful join', () => {
    roomApi.joinRoom.and.returnValue(of({ id: 42, createdAt: '2025-01-01T00:00:00Z' } as Room));

    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.joinRoomCodeInput = '42';
    component.joinRoom();

    expect(router.navigate).toHaveBeenCalledWith(['/rooms', 42]);
  });

  it('should update paging boundaries with previous and next actions', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.rooms = Array.from({ length: 13 }).map((_, index) => ({
      id: index + 1,
      createdAt: '2025-01-01T00:00:00Z'
    } as Room));

    expect(component.totalPages).toBe(3);
    expect(component.currentPage).toBe(1);

    component.nextPage();
    component.nextPage();
    component.nextPage();
    expect(component.currentPage).toBe(3);

    component.previousPage();
    expect(component.currentPage).toBe(2);
  });
});


