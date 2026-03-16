/*
renders study room collaboration ui and coordinates tasks chat timer and music
*/
import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { RoomApiService, TimerStateResponse } from '../../core/services/room-api.service';
import { TaskApiService } from '../../core/services/task-api.service';
import { WsService } from '../../core/services/ws.service';
import { RoomMessage } from '../../shared/models/message.model';
import { RoomDetail } from '../../shared/models/room.model';
import { Task } from '../../shared/models/task.model';
import { RoomEvent } from '../../shared/models/ws.model';

interface FeedItem {
  id: string;
  type: 'chat' | 'system';
  sender?: string;
  body: string;
  own?: boolean;
}

@Component({
  selector: 'app-study-room',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <p *ngIf="entryState === 'loading'" class="text-sm">Loading room...</p>

    <section *ngIf="entryState === 'forbidden'" class="mb-4 border border-zinc-600 p-4">
      <p class="text-red-700">You are not a member of this room.</p>
      <div class="mt-2 flex gap-2">
        <button
          type="button"
          (click)="joinRoomExplicitly()"
          [disabled]="joining"
          class="border border-zinc-600 px-3 py-1 text-sm hover:bg-zinc-200 disabled:opacity-60"
        >
          {{ joining ? 'Joining...' : 'Join Room' }}
        </button>
        <button type="button" (click)="goToDashboard()" class="border border-zinc-600 px-3 py-1 text-sm hover:bg-zinc-200">Back to Dashboard</button>
      </div>
      <p *ngIf="entryError" class="mt-2 text-sm text-red-700">{{ entryError }}</p>
    </section>

    <section *ngIf="entryState === 'not-found'" class="mb-4 border border-zinc-600 p-4">
      <p class="text-red-700">Room not found.</p>
      <button type="button" (click)="goToDashboard()" class="mt-2 border border-zinc-600 px-3 py-1 text-sm hover:bg-zinc-200">Back to Dashboard</button>
    </section>

    <section *ngIf="entryState === 'error'" class="mb-4 border border-zinc-600 p-4">
      <p class="text-red-700">Unable to load room right now.</p>
      <button type="button" (click)="goToDashboard()" class="mt-2 border border-zinc-600 px-3 py-1 text-sm hover:bg-zinc-200">Back to Dashboard</button>
    </section>

    <ng-container *ngIf="entryState === 'ready'">
      <section class="border border-stone-400 bg-stone-100 p-3">
        <div class="mb-3 flex items-center justify-between border-b border-stone-400 pb-3">
          <div>
            <h2 class="text-2xl font-normal text-stone-900">{{ roomDetail?.room?.name || 'Room Name' }}</h2>
            <p class="text-xs text-stone-500">Room ID: {{ roomDetail?.room?.id }}</p>
          </div>
          <div class="flex items-center gap-2">
            <button
              *ngIf="isOwner; else leaveButton"
              type="button"
              (click)="deleteRoom()"
              class="rounded-full border border-red-700 bg-red-100 px-3 py-1 text-xs font-semibold text-red-800 hover:bg-red-200"
            >
              DELETE ROOM
            </button>
            <ng-template #leaveButton>
              <button
                type="button"
                (click)="leaveRoom()"
                class="rounded-full border border-orange-700 bg-orange-100 px-3 py-1 text-xs font-semibold text-orange-800 hover:bg-orange-200"
              >
                LEAVE ROOM
              </button>
            </ng-template>
          </div>
        </div>

        <p *ngIf="leaveError" class="mb-2 text-sm text-red-700">{{ leaveError }}</p>

        <div class="grid gap-3 lg:grid-cols-[1fr_1fr_0.8fr]">
          <section class="flex h-[560px] flex-col overflow-hidden border border-stone-400 bg-stone-50 p-3">
            <h3 class="mb-3 text-2xl font-medium">Tasks</h3>

            <ul #taskList class="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
              <li *ngFor="let task of tasks" class="flex items-start justify-between gap-2 text-lg">
                <label class="flex min-w-0 items-start gap-2">
                  <input type="checkbox" [checked]="task.done" (change)="toggleTaskDone(task)" class="mt-1 h-4 w-4 border-stone-400" />
                  <span class="break-words" [class.line-through]="task.done">{{ task.title }}</span>
                </label>
                <button type="button" (click)="deleteTask(task.id)" class="text-xs font-semibold uppercase text-stone-600 hover:underline">Delete</button>
              </li>
            </ul>

            <div class="mt-3 flex gap-2">
              <input
                [(ngModel)]="newTaskTitle"
                placeholder="Add a new task..."
                class="min-w-0 flex-1 rounded-full border border-stone-400 bg-white px-3 py-2 text-sm"
              />
              <button
                type="button"
                (click)="createTask()"
                [disabled]="!newTaskTitle.trim()"
                class="rounded-full border border-stone-500 px-4 py-1.5 text-xs font-semibold hover:bg-stone-200 disabled:opacity-50"
              >
                ADD
              </button>
            </div>

            <p *ngIf="taskError" class="mt-2 text-sm text-red-700">{{ taskError }}</p>

            <section class="mt-3 border border-stone-400 p-2">
              <p class="mb-2 text-center text-2xl font-medium">Ambient Music</p>
              <div class="flex flex-wrap items-center justify-center gap-2">
                <button
                  type="button"
                  (click)="toggleMusicPlayback()"
                  class="rounded-full border border-stone-400 px-3 py-1 text-xs font-semibold hover:bg-stone-200 disabled:opacity-50"
                >
                  {{ musicPlaying ? 'Pause Music' : 'Play Music' }}
                </button>
                <input
                  id="music-volume"
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  [ngModel]="musicVolume"
                  (ngModelChange)="onMusicVolumeChange($event)"
                  class="w-24 accent-teal-700"
                />
                <span class="text-xs">{{ musicVolumePercent }}%</span>
              </div>
              <p *ngIf="musicAutoplayBlocked" class="mt-1 text-center text-xs text-stone-700">Browser autoplay is blocked. Click Play Music to start audio.</p>
            </section>
          </section>

          <section class="flex h-[560px] flex-col overflow-hidden border border-stone-400 bg-stone-50 p-3">
            <h3 class="mb-2 text-2xl font-medium">Chat</h3>

            <ul #chatFeed class="min-h-0 flex-1 space-y-1 overflow-y-auto border border-stone-300 bg-white p-2 text-sm">
              <li *ngFor="let item of feedItems; trackBy: trackFeedItem" class="break-words">
                <ng-container *ngIf="item.type === 'chat'; else systemMessage">
                  <span class="font-semibold" [class.text-teal-700]="item.own">{{ item.sender }}:</span>
                  <span>{{ item.body }}</span>
                </ng-container>
                <ng-template #systemMessage>
                  <span class="text-xs italic text-stone-500">{{ item.body }}</span>
                </ng-template>
              </li>
            </ul>

            <div class="mt-3 flex items-center gap-2">
              <input
                [(ngModel)]="chatMessage"
                (keydown.enter)="sendChat()"
                placeholder="Enter Message..."
                class="h-10 min-w-0 flex-1 rounded-full border border-stone-400 bg-white px-4"
              />
              <button
                type="button"
                (click)="sendChat()"
                [disabled]="!chatMessage.trim()"
                class="rounded-full border border-stone-500 px-4 py-1.5 text-xs font-semibold hover:bg-stone-200 disabled:opacity-50"
              >
                SEND
              </button>
            </div>

            <p *ngIf="chatError" class="mt-2 text-sm text-red-700">{{ chatError }}</p>
          </section>

          <section class="flex h-[560px] flex-col gap-3 overflow-hidden border border-stone-400 bg-stone-50 p-3">
            <div class="border border-stone-400 bg-white p-3">
              <h3 class="mb-2 text-center text-2xl font-medium">Timer</h3>
              <p class="w-full text-center text-6xl font-semibold leading-none tracking-wide text-stone-900">{{ formatElapsed(displayRemainingSeconds).slice(3) }}</p>
              <p class="mt-2 text-center text-sm font-semibold tracking-wide text-stone-700">{{ timerState.isRunning ? 'RUNNING' : 'PAUSED' }}</p>
              <p class="mt-3 text-sm uppercase tracking-wide"><strong>Current Phase: {{ pomodoroPhase }}</strong></p>
              <p class="text-sm uppercase tracking-wide">Next Phase: {{ pomodoroPhase === 'FOCUS' ? 'BREAK' : 'FOCUS' }}</p>

              <div *ngIf="canControlTimer" class="mt-3 flex flex-nowrap gap-2">
                <button
                  type="button"
                  (click)="startTimer()"
                  [disabled]="timerState.isRunning || !canControlTimer"
                  class="flex-1 rounded-full border border-stone-500 px-2 py-1 text-xs font-semibold hover:bg-stone-200 disabled:opacity-50"
                >
                  START
                </button>
                <button
                  type="button"
                  (click)="pauseTimer()"
                  [disabled]="!timerState.isRunning || !canControlTimer"
                  class="flex-1 rounded-full border border-stone-500 px-2 py-1 text-xs font-semibold hover:bg-stone-200 disabled:opacity-50"
                >
                  PAUSE
                </button>
                <button
                  type="button"
                  (click)="resetTimer()"
                  [disabled]="!canControlTimer"
                  class="flex-1 rounded-full border border-stone-500 px-2 py-1 text-xs font-semibold hover:bg-stone-200 disabled:opacity-50"
                >
                  RESET
                </button>
              </div>
              <p *ngIf="timerError" class="mt-2 text-xs text-red-700">{{ timerError }}</p>
            </div>

            <div class="flex min-h-0 flex-1 flex-col overflow-hidden border border-stone-400 bg-white p-3">
              <h3 class="mb-2 text-center text-2xl font-medium">Participants</h3>
              <ul class="min-h-0 flex-1 space-y-1 overflow-y-auto text-2xl leading-tight">
                <li *ngFor="let member of roomDetail?.members">{{ member.displayName }}</li>
              </ul>
              <p class="mt-2 text-right text-sm text-stone-700">Members: {{ roomDetail?.members?.length || 0 }}</p>
            </div>
          </section>
        </div>
      </section>
    </ng-container>
  `
})
export class StudyRoomComponent implements OnInit, OnDestroy {
  @ViewChild('chatFeed') private chatFeedRef?: ElementRef<HTMLUListElement>;
  @ViewChild('taskList') private taskListRef?: ElementRef<HTMLUListElement>;

  entryState: 'loading' | 'ready' | 'forbidden' | 'not-found' | 'error' = 'loading';
  entryError = '';
  joining = false;

  roomId = 0;
  currentUserId: number | null = null;
  canControlTimer = false;
  roomDetail?: RoomDetail;
  messageHistory: RoomMessage[] = [];
  tasks: Task[] = [];
  feedItems: FeedItem[] = [];
  events: RoomEvent[] = [];
  newTaskTitle = '';
  chatMessage = '';
  chatError = '';
  taskError = '';
  timerError = '';
  leaveError = '';
  pomodoroPhase: 'FOCUS' | 'BREAK' = 'FOCUS';
  displayRemainingSeconds = 1500;
  musicPlaying = false;
  musicVolume = 0.35;
  musicAutoplayBlocked = false;
  timerState: TimerStateResponse = {
    isRunning: false,
    elapsedSeconds: 0,
    startTime: undefined,
    phase: 'FOCUS',
    phaseDurationSeconds: 1500,
    remainingSeconds: 1500
  };

  private wsSubscription?: Subscription;
  private wsErrorSubscription?: Subscription;
  private pendingChatMessage = '';
  private renderTimerId?: ReturnType<typeof setInterval>;
  private ambientAudio?: HTMLAudioElement;
  private readonly ambientTrackUrl = '/lofi.mp3';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private roomApi: RoomApiService,
    private taskApi: TaskApiService,
    private wsService: WsService
  ) {}

  // resolves route room id and starts the room entry flow
  ngOnInit(): void {
    this.roomId = Number(this.route.snapshot.paramMap.get('roomId'));
    this.currentUserId = this.authService.getCurrentUserId();
    this.loadMusicPreferences();
    if (!this.roomId || Number.isNaN(this.roomId)) {
      this.entryState = 'not-found';
      return;
    }
    this.tryEnterRoom();
  }

  // clears subscriptions and realtime resources when leaving the view
  ngOnDestroy(): void {
    this.teardownRealtime();
  }

  // creates a room task then reloads task data
  createTask(): void {
    this.taskError = '';
    const title = this.newTaskTitle.trim();
    if (!title) {
      return;
    }

    this.taskApi.createTask(this.roomId, title).subscribe({
      next: () => {
        this.newTaskTitle = '';
        this.loadTasks();
      },
      error: err => {
        this.taskError = err?.error?.message ?? 'Unable to create task.';
      }
    });
  }

  // toggles task completion and applies updated task data
  toggleTaskDone(task: Task): void {
    this.taskError = '';
    this.taskApi.updateTask(this.roomId, task.id, { done: !task.done }).subscribe({
      next: updated => {
        this.applyTaskUpdate('updated', updated);
      },
      error: err => {
        this.taskError = err?.error?.message ?? 'Unable to update task.';
      }
    });
  }

  // deletes a task and applies local task update state
  deleteTask(taskId: number): void {
    this.taskError = '';
    this.taskApi.deleteTask(this.roomId, taskId).subscribe({
      next: deleted => {
        this.applyTaskUpdate('deleted', deleted);
      },
      error: err => {
        this.taskError = err?.error?.message ?? 'Unable to delete task.';
      }
    });
  }

  // sends chat text over websocket and clears the input field
  sendChat(): void {
    const body = this.chatMessage.trim();
    if (!body) {
      return;
    }
    this.chatError = '';
    this.pendingChatMessage = body;
    this.wsService.sendChat(this.roomId, body);
    this.chatMessage = '';
    this.scheduleChatScrollToBottom();
  }

  // leaves the current room after user confirmation
  leaveRoom(): void {
    if (!window.confirm('Leave this room?')) {
      return;
    }

    this.leaveError = '';
    this.roomApi.leaveRoom(this.roomId).subscribe({
      next: () => {
        this.teardownRealtime();
        this.goToDashboard();
      },
      error: err => {
        this.leaveError = err?.error?.message ?? 'Failed to leave room.';
      }
    });
  }

  // deletes the room when the current user is the owner
  deleteRoom(): void {
    if (!window.confirm('Delete this room for all members? This cannot be undone.')) {
      return;
    }

    this.leaveError = '';
    this.roomApi.deleteRoom(this.roomId).subscribe({
      next: () => {
        this.teardownRealtime();
        this.goToDashboard();
      },
      error: err => {
        this.leaveError = err?.error?.message ?? 'Failed to delete room.';
      }
    });
  }

  // executes explicit join flow from forbidden room entry state
  joinRoomExplicitly(): void {
    this.entryError = '';
    this.joining = true;
    this.roomApi.joinRoom(this.roomId).subscribe({
      next: () => {
        this.joining = false;
        this.tryEnterRoom();
      },
      error: err => {
        this.joining = false;
        this.entryError = err?.error?.message ?? 'Unable to join room.';
      }
    });
  }

  // routes back to the dashboard screen
  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  // starts timer control for owner users
  startTimer(): void {
    this.timerError = '';
    this.roomApi.startTimer(this.roomId).subscribe({
      next: state => {
        this.applyTimerState(state);
      },
      error: err => {
        this.timerError = err?.error?.message ?? 'Unable to start timer.';
      }
    });
  }

  // pauses timer control for owner users
  pauseTimer(): void {
    this.timerError = '';
    this.roomApi.pauseTimer(this.roomId).subscribe({
      next: state => {
        this.applyTimerState(state);
      },
      error: err => {
        this.timerError = err?.error?.message ?? 'Unable to pause timer.';
      }
    });
  }

  // resets timer control for owner users
  resetTimer(): void {
    this.timerError = '';
    this.roomApi.resetTimer(this.roomId).subscribe({
      next: state => {
        this.applyTimerState(state);
      },
      error: err => {
        this.timerError = err?.error?.message ?? 'Unable to reset timer.';
      }
    });
  }

  formatElapsed(totalSeconds: number): string {
    const safe = Math.max(0, totalSeconds);
    const hours = Math.floor(safe / 3600);
    const minutes = Math.floor((safe % 3600) / 60);
    const seconds = safe % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  get musicVolumePercent(): number {
    return Math.round(this.musicVolume * 100);
  }

  toggleMusicPlayback(): void {
    if (this.musicPlaying) {
      this.musicPlaying = false;
    } else {
      this.musicPlaying = true;
    }
    this.syncMusicPlayback();
    this.persistMusicPreferences();
  }

  onMusicVolumeChange(value: number | string): void {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return;
    }

    this.musicVolume = Math.min(1, Math.max(0, parsed));
    this.applyAudioSettings();
    if (this.musicAutoplayBlocked) {
      this.syncMusicPlayback();
    }
    this.persistMusicPreferences();
  }

  // loads room details and decides ready forbidden not found or error states
  private tryEnterRoom(): void {
    this.entryState = 'loading';
    this.entryError = '';
    this.teardownRealtime();

    this.roomApi.getRoom(this.roomId).subscribe({
      next: room => {
        this.roomDetail = room;
        this.canControlTimer = this.currentUserId !== null && room.room.ownerId === this.currentUserId;
        this.applyTimerState({
          isRunning: room.room.isRunning,
          elapsedSeconds: room.room.elapsedSeconds,
          startTime: room.room.startTime,
          phase: room.room.breakPhase ? 'BREAK' : 'FOCUS',
          phaseDurationSeconds: room.room.breakPhase ? 300 : 1500,
          remainingSeconds: 0
        });
        this.entryState = 'ready';
        this.loadRoomInternals();
      },
      error: err => {
        const status = Number(err?.status ?? 0);
        if (status === 403) {
          this.entryState = 'forbidden';
          return;
        }
        if (status === 404) {
          this.entryState = 'not-found';
          return;
        }
        this.entryState = 'error';
      }
    });
  }

  // loads initial room internals and subscribes to realtime event streams
  private loadRoomInternals(): void {
    this.roomApi.listMessages(this.roomId).subscribe(messages => {
      this.messageHistory = messages;
      this.feedItems = messages.map((message, index) => this.toFeedChatItem(message, index));
      this.scheduleChatScrollToBottom();
    });
    this.loadTasks();

    this.wsService.connect(this.roomId);
    this.wsSubscription = this.wsService.events$.subscribe(event => {
        this.events = [event, ...this.events];

        if (event.type === 'timer_update') {
          this.pushSystemFeed(this.formatFeedLine(event));
          this.scheduleChatScrollToBottom();
          const payload = event.payload;
          this.applyTimerState({
            isRunning: payload.isRunning,
            elapsedSeconds: payload.elapsedSeconds,
            startTime: payload.startTime,
            phase: payload.phase ?? this.pomodoroPhase,
            phaseDurationSeconds: payload.phaseDurationSeconds ?? this.timerState.phaseDurationSeconds,
            remainingSeconds: payload.remainingSeconds ?? 0
          });
        } else if (event.type === 'task_update') {
          this.pushSystemFeed(this.formatFeedLine(event));
          this.scheduleChatScrollToBottom();
          const payload = event.payload;
          const action = payload.action;
          const task = payload.task;
          if (task) {
            this.applyTaskUpdate(action, task);
            this.scheduleTaskScrollToBottom();
          }
        } else if (event.type === 'chat_message') {
          const message: RoomMessage = {
            id: 0,
            roomId: event.roomId,
            userId: 0,
            sender: event.payload.sender,
            body: event.payload.body,
            createdAt: event.timestamp
          };
          if (this.pendingChatMessage && message.body === this.pendingChatMessage) {
            this.pendingChatMessage = '';
          }
          this.messageHistory = [...this.messageHistory, message];
          this.feedItems = [...this.feedItems, this.toFeedChatItem(message, this.feedItems.length)].slice(-300);
          this.scheduleChatScrollToBottom();
        } else {
          this.pushSystemFeed(this.formatFeedLine(event));
          this.scheduleChatScrollToBottom();
        }
    });

    this.wsErrorSubscription = this.wsService.errors$.subscribe(message => {
      this.chatError = message;
      if (!this.chatMessage.trim() && this.pendingChatMessage) {
        this.chatMessage = this.pendingChatMessage;
      }
      this.pendingChatMessage = '';
    });

    this.syncMusicPlayback();

    this.renderTimerId = setInterval(() => {
      this.refreshDisplayRemaining();
    }, 1000);
  }

  // tears down websocket subscriptions timer render loop and audio playback
  private teardownRealtime(): void {
    this.wsSubscription?.unsubscribe();
    this.wsSubscription = undefined;
    this.wsErrorSubscription?.unsubscribe();
    this.wsErrorSubscription = undefined;
    this.wsService.disconnect();
    this.ambientAudio?.pause();
    if (this.renderTimerId) {
      clearInterval(this.renderTimerId);
      this.renderTimerId = undefined;
    }
  }

  // applies timer payload to local component timer state
  private applyTimerState(state: TimerStateResponse): void {
    this.timerState = {
      isRunning: state.isRunning,
      elapsedSeconds: state.elapsedSeconds,
      startTime: state.startTime,
      phase: state.phase,
      phaseDurationSeconds: state.phaseDurationSeconds,
      remainingSeconds: state.remainingSeconds
    };
    this.pomodoroPhase = state.phase;
    this.refreshDisplayRemaining();
  }

  // computes displayed remaining time using current timer mode and start time
  private refreshDisplayRemaining(): void {
    if (!this.timerState.isRunning || !this.timerState.startTime) {
      this.displayRemainingSeconds = Math.max(0, this.timerState.phaseDurationSeconds - this.timerState.elapsedSeconds);
      return;
    }

    const start = new Date(this.timerState.startTime).getTime();
    if (Number.isNaN(start)) {
      this.displayRemainingSeconds = Math.max(0, this.timerState.phaseDurationSeconds - this.timerState.elapsedSeconds);
      return;
    }

    const delta = Math.max(0, Math.floor((Date.now() - start) / 1000));
    const currentElapsed = this.timerState.elapsedSeconds + delta;
    this.displayRemainingSeconds = Math.max(0, this.timerState.phaseDurationSeconds - currentElapsed);
  }

  // merges task update events into local task list state
  private applyTaskUpdate(action: string, task: Task): void {
    if (task.roomId !== this.roomId) {
      return;
    }

    if (action === 'deleted') {
      this.tasks = this.tasks.filter(existing => existing.id !== task.id);
      return;
    }

    const existingIndex = this.tasks.findIndex(existing => existing.id === task.id);
    if (existingIndex >= 0) {
      const updated = [...this.tasks];
      updated[existingIndex] = task;
      this.tasks = updated;
    } else {
      this.tasks = [...this.tasks, task];
    }
  }

  // loads task list data from the room task endpoint
  private loadTasks(): void {
    this.taskApi.listTasks(this.roomId).subscribe({
      next: tasks => {
        this.tasks = tasks.filter(task => task.roomId === this.roomId);
        this.scheduleTaskScrollToBottom();
      },
      error: err => {
        this.taskError = err?.error?.message ?? 'Unable to load tasks.';
      }
    });
  }

  private formatFeedLine(event: RoomEvent): string {
    const time = this.formatTimestamp(event.timestamp);

    if (event.type === 'timer_update') {
      const payload = event.payload;
      const isRunning = payload.isRunning;
      const elapsedSeconds = payload.elapsedSeconds;
      const hasStartTime = typeof payload.startTime === 'string';

      if (isRunning && hasStartTime && elapsedSeconds === 0) {
        return `${time} Timer started`;
      }
      if (isRunning) {
        return `${time} Timer resumed`;
      }
      if (!isRunning && elapsedSeconds === 0) {
        return `${time} Timer reset`;
      }
      return `${time} Timer paused at ${this.formatElapsed(elapsedSeconds)}`;
    }

    if (event.type === 'task_update') {
      const action = event.payload.action;
      const title = event.payload.task?.title ?? 'task';

      if (action === 'added') {
        return `${time} Task added: ${title}`;
      }
      if (action === 'deleted') {
        return `${time} Task deleted: ${title}`;
      }
      return `${time} Task updated: ${title}`;
    }

    if (event.type === 'chat_message') {
      const sender = event.payload.sender;
      const body = event.payload.body;
      return `${time} ${sender}: ${body}`;
    }

    return `${time} ${event.type}`;
  }

  private formatTimestamp(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  private formatHistoricalMessage(message: RoomMessage): string {
    const time = this.formatTimestamp(message.createdAt);
    return `${time} ${message.sender}: ${message.body}`;
  }

  private toFeedChatItem(message: RoomMessage, index: number): FeedItem {
    return {
      id: `chat-${message.id}-${index}`,
      type: 'chat',
      sender: message.sender,
      body: message.body,
      own: this.isOwnSender(message.sender)
    };
  }

  private pushSystemFeed(body: string): void {
    const item: FeedItem = {
      id: `system-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      type: 'system',
      body
    };
    this.feedItems = [...this.feedItems, item].slice(-300);
  }

  private isOwnSender(sender: string): boolean {
    try {
      const raw = localStorage.getItem('vsr_user');
      if (!raw) {
        return false;
      }
      const parsed = JSON.parse(raw) as { displayName?: string };
      const displayName = parsed.displayName?.trim().toLowerCase();
      return !!displayName && sender.trim().toLowerCase() === displayName;
    } catch {
      return false;
    }
  }

  trackFeedItem(_: number, item: FeedItem): string {
    return item.id;
  }

  get isOwner(): boolean {
    return !!this.roomDetail && this.currentUserId !== null && this.roomDetail.room.ownerId === this.currentUserId;
  }

  private syncMusicPlayback(): void {
    if (!this.ambientAudio) {
      this.ambientAudio = new Audio(this.ambientTrackUrl);
      this.ambientAudio.loop = true;
      this.ambientAudio.preload = 'auto';
    }

    this.applyAudioSettings();

    if (!this.musicPlaying) {
      this.ambientAudio.pause();
      this.musicAutoplayBlocked = false;
      return;
    }

    this.ambientAudio.play().then(() => {
      this.musicAutoplayBlocked = false;
      this.musicPlaying = true;
    }).catch(() => {
      this.musicAutoplayBlocked = true;
      this.musicPlaying = false;
    });
  }

  private applyAudioSettings(): void {
    if (!this.ambientAudio) {
      return;
    }

    this.ambientAudio.volume = this.musicVolume;
  }

  private loadMusicPreferences(): void {
    try {
      const raw = localStorage.getItem(this.musicPreferenceKey());
      if (!raw) {
        return;
      }

      const parsed = JSON.parse(raw) as {
        volume?: number;
      };

      if (typeof parsed.volume === 'number') {
        this.musicVolume = Math.min(1, Math.max(0, parsed.volume));
      }
    } catch {
      // Ignore malformed local preference state and use defaults.
    }
  }

  private persistMusicPreferences(): void {
    const payload = {
      volume: this.musicVolume
    };
    localStorage.setItem(this.musicPreferenceKey(), JSON.stringify(payload));
  }

  private scheduleChatScrollToBottom(): void {
    this.runAfterRender(() => {
      const el = this.chatFeedRef?.nativeElement;
      if (!el) {
        return;
      }
      el.scrollTop = el.scrollHeight;
    });
  }

  private scheduleTaskScrollToBottom(): void {
    this.runAfterRender(() => {
      const el = this.taskListRef?.nativeElement;
      if (!el) {
        return;
      }
      el.scrollTop = el.scrollHeight;
    });
  }

  private runAfterRender(action: () => void): void {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        action();
      });
    });
  }

  private musicPreferenceKey(): string {
    const scope = this.currentUserId !== null ? this.currentUserId.toString() : 'anonymous';
    return `vsr_music_prefs_${scope}`;
  }
}

