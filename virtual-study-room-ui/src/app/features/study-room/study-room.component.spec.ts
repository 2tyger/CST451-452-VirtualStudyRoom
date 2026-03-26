/*
renders study room collaboration ui and coordinates tasks chat timer and music
*/
import { of, Subject, throwError } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { StudyRoomComponent } from './study-room.component';
import { AuthService } from '../../core/services/auth.service';
import { RoomApiService } from '../../core/services/room-api.service';
import { TaskApiService } from '../../core/services/task-api.service';
import { WsService } from '../../core/services/ws.service';

describe('StudyRoomComponent', () => {
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let taskApi: jasmine.SpyObj<TaskApiService>;
  let wsService: {
    events$: Subject<unknown>;
    errors$: Subject<string>;
    connect: jasmine.Spy;
    disconnect: jasmine.Spy;
    sendChat: jasmine.Spy;
  };

  beforeEach(async () => {
    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', [
      'getRoom',
      'listMessages',
      'joinRoom',
      'leaveRoom',
      'deleteRoom',
      'startTimer',
      'pauseTimer',
      'resetTimer'
    ]);
    roomApi.getRoom.and.returnValue(of({
      room: {
        id: 1,
        name: 'Room',
        ownerId: 1,
        memberCount: 1,
        active: true,
        breakPhase: false,
        isRunning: false,
        elapsedSeconds: 0,
        createdAt: '2025-01-01T00:00:00Z'
      },
      members: [{ userId: 1, displayName: 'Ty', role: 'OWNER' }]
    }));
    roomApi.listMessages.and.returnValue(of([]));
    roomApi.joinRoom.and.returnValue(of({
      id: 1,
      name: 'Room',
      ownerId: 1,
      memberCount: 1,
      active: true,
      breakPhase: false,
      isRunning: false,
      elapsedSeconds: 0,
      createdAt: '2025-01-01T00:00:00Z'
    }));
    roomApi.leaveRoom.and.returnValue(of({
      id: 1,
      name: 'Room',
      ownerId: 1,
      memberCount: 1,
      active: true,
      breakPhase: false,
      isRunning: false,
      elapsedSeconds: 0,
      createdAt: '2025-01-01T00:00:00Z'
    }));
    roomApi.deleteRoom.and.returnValue(of(void 0));
    roomApi.startTimer.and.returnValue(of({
      isRunning: true,
      elapsedSeconds: 0,
      phase: 'FOCUS',
      phaseDurationSeconds: 1500,
      remainingSeconds: 1500
    }));
    roomApi.pauseTimer.and.returnValue(of({
      isRunning: false,
      elapsedSeconds: 5,
      phase: 'FOCUS',
      phaseDurationSeconds: 1500,
      remainingSeconds: 1495
    }));
    roomApi.resetTimer.and.returnValue(of({
      isRunning: false,
      elapsedSeconds: 0,
      phase: 'FOCUS',
      phaseDurationSeconds: 1500,
      remainingSeconds: 1500
    }));

    taskApi = jasmine.createSpyObj<TaskApiService>('TaskApiService', ['listTasks', 'createTask', 'updateTask', 'deleteTask']);
    taskApi.listTasks.and.returnValue(of([]));
    taskApi.createTask.and.returnValue(of({
      id: 1,
      roomId: 1,
      title: 'Task',
      done: false,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z'
    }));
    taskApi.updateTask.and.returnValue(of({
      id: 1,
      roomId: 1,
      title: 'Task',
      done: true,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z'
    }));
    taskApi.deleteTask.and.returnValue(of({
      id: 1,
      roomId: 1,
      title: 'Task',
      done: true,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z'
    }));

    wsService = {
      events$: new Subject<unknown>(),
      errors$: new Subject<string>(),
      connect: jasmine.createSpy('connect'),
      disconnect: jasmine.createSpy('disconnect'),
      sendChat: jasmine.createSpy('sendChat')
    };

    await TestBed.configureTestingModule({
      imports: [StudyRoomComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ roomId: '1' })
            }
          }
        },
        {
          provide: Router,
          useValue: {
            navigate: jasmine.createSpy('navigate')
          }
        },
        {
          provide: AuthService,
          useValue: {
            getCurrentUserId: () => 1
          }
        },
        {
          provide: RoomApiService,
          useValue: roomApi
        },
        {
          provide: TaskApiService,
          useValue: taskApi
        },
        {
          provide: WsService,
          useValue: wsService
        }
      ]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should set forbidden entry state on 403 room load', () => {
    roomApi.getRoom.and.returnValue(throwError(() => ({ status: 403 })));

    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.entryState).toBe('forbidden');
  });

  it('should send chat and clear input', () => {
    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.chatMessage = 'hello';
    component.sendChat();

    expect(wsService.sendChat).toHaveBeenCalledWith(1, 'hello');
    expect(component.chatMessage).toBe('');
  });

  it('should join room explicitly and retry enter flow', () => {
    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const enterSpy = spyOn(component as never, 'tryEnterRoom');

    component.joinRoomExplicitly();

    expect(roomApi.joinRoom).toHaveBeenCalledWith(1);
    expect(enterSpy).toHaveBeenCalled();
  });

  it('should stop leave flow when confirmation is cancelled', () => {
    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(false);

    component.leaveRoom();

    expect(roomApi.leaveRoom).not.toHaveBeenCalled();
  });

  it('should leave room and navigate when confirmed', () => {
    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(true);

    component.leaveRoom();

    expect(roomApi.leaveRoom).toHaveBeenCalledWith(1);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});


