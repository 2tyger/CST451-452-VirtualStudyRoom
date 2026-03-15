import { NEVER } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { StudyRoomComponent } from './study-room.component';
import { AuthService } from '../../core/services/auth.service';
import { RoomApiService } from '../../core/services/room-api.service';
import { TaskApiService } from '../../core/services/task-api.service';
import { WsService } from '../../core/services/ws.service';

describe('StudyRoomComponent', () => {
  beforeEach(async () => {
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
          useValue: {}
        },
        {
          provide: TaskApiService,
          useValue: {}
        },
        {
          provide: WsService,
          useValue: {
            events$: NEVER,
            errors$: NEVER,
            connect: () => {},
            disconnect: () => {},
            sendChat: () => {}
          }
        }
      ]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(StudyRoomComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
