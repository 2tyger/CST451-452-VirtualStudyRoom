import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RoomApiService } from '../../core/services/room-api.service';
import { Room } from '../../shared/models/room.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="mx-auto max-w-[920px]">
      <h2 class="mb-6 text-center text-4xl font-normal text-stone-900">Welcome Back, {{ currentDisplayName }}!</h2>

      <div class="mb-4 flex flex-wrap items-center gap-2">
        <button
          type="button"
          (click)="openCreateModal()"
          class="rounded-full border border-stone-500 bg-stone-100 px-4 py-2 text-sm font-medium hover:bg-stone-200"
        >
          + Create Room
        </button>
        <button
          type="button"
          (click)="openJoinModal()"
          class="rounded-full border border-stone-500 bg-stone-100 px-4 py-2 text-sm font-medium hover:bg-stone-200"
        >
          Join by Room ID
        </button>
      </div>

      <p *ngIf="joinError" class="mb-3 text-sm text-red-700">{{ joinError }}</p>
      <p *ngIf="error" class="mb-3 text-sm text-red-700">{{ error }}</p>

      <div class="space-y-3 border border-stone-400 bg-stone-100 p-3">
        <article
          *ngFor="let room of pagedRooms"
          class="flex items-start justify-between border border-stone-400 bg-stone-50 p-3"
        >
          <button type="button" (click)="openRoom(room.id)" class="text-left">
            <p class="text-base leading-tight">{{ room.name }}</p>
            <p class="mt-0.5 text-xs text-stone-500">Room ID: {{ room.id }}</p>
            <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-stone-700">
              <span>{{ room.memberCount }} users</span>
              <span *ngIf="isOwnedByCurrentUser(room)" class="rounded-full bg-teal-100 px-2 py-0.5 font-semibold text-teal-800">OWNER</span>
            </div>
          </button>
          <span
            class="rounded-full border px-4 py-1 text-xs font-bold tracking-wide"
            [class.border-teal-700]="room.isRunning"
            [class.bg-teal-100]="room.isRunning"
            [class.text-teal-800]="room.isRunning"
            [class.border-stone-500]="!room.isRunning"
            [class.bg-stone-100]="!room.isRunning"
            [class.text-stone-700]="!room.isRunning"
          >
            {{ room.isRunning ? 'IN SESSION' : 'IDLE' }}
          </span>
        </article>

        <p *ngIf="rooms.length === 0" class="p-2 text-sm text-stone-700">No rooms yet. Create one to get started.</p>

        <div *ngIf="totalPages > 1" class="flex items-center justify-between border-t border-stone-400 pt-3 text-sm">
          <button
            type="button"
            (click)="previousPage()"
            [disabled]="currentPage === 1"
            class="rounded-full border border-stone-500 px-3 py-1 hover:bg-stone-200 disabled:opacity-50"
          >
            Previous
          </button>
          <p>Page {{ currentPage }} of {{ totalPages }}</p>
          <button
            type="button"
            (click)="nextPage()"
            [disabled]="currentPage === totalPages"
            class="rounded-full border border-stone-500 px-3 py-1 hover:bg-stone-200 disabled:opacity-50"
          >
            Next
          </button>
        </div>
      </div>

      <div *ngIf="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/35 p-4">
        <div class="w-full max-w-md border border-stone-500 bg-stone-50 p-4 shadow-lg">
          <h3 class="mb-3 text-xl font-medium">Create Room</h3>
          <input
            [(ngModel)]="newRoomName"
            placeholder="Enter room name"
            class="mb-3 h-10 w-full border border-stone-500 bg-white px-3"
          />
          <div class="flex justify-end gap-2">
            <button type="button" (click)="closeCreateModal()" class="border border-stone-500 px-3 py-1.5 text-sm hover:bg-stone-200">Cancel</button>
            <button
              type="button"
              (click)="createRoom()"
              [disabled]="!newRoomName.trim()"
              class="border border-stone-500 bg-stone-100 px-3 py-1.5 text-sm font-semibold hover:bg-stone-200 disabled:opacity-50"
            >
              Create
            </button>
          </div>
        </div>
      </div>

      <div *ngIf="showJoinModal" class="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/35 p-4">
        <div class="w-full max-w-md border border-stone-500 bg-stone-50 p-4 shadow-lg">
          <h3 class="mb-3 text-xl font-medium">Join by Room ID</h3>
          <input
            [ngModel]="joinRoomCodeInput"
            (ngModelChange)="onJoinCodeInput($event)"
            inputmode="numeric"
            pattern="[0-9]*"
            autocomplete="off"
            placeholder="Enter Room ID"
            class="mb-3 h-10 w-full border border-stone-500 bg-white px-3"
          />
          <div class="flex justify-end gap-2">
            <button type="button" (click)="closeJoinModal()" class="border border-stone-500 px-3 py-1.5 text-sm hover:bg-stone-200">Cancel</button>
            <button
              type="button"
              (click)="joinRoom()"
              [disabled]="joining || !joinRoomCodeInput"
              class="border border-stone-500 bg-stone-100 px-3 py-1.5 text-sm font-semibold hover:bg-stone-200 disabled:opacity-50"
            >
              {{ joining ? 'Joining...' : 'Join' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  `
})
export class DashboardComponent implements OnInit {
  rooms: Room[] = [];
  currentPage = 1;
  readonly pageSize = 6;
  newRoomName = '';
  joinRoomCodeInput = '';
  showCreateModal = false;
  showJoinModal = false;
  joining = false;
  joinError = '';
  error = '';
  currentDisplayName = 'Student';

  constructor(private roomApi: RoomApiService, private router: Router) {}

  ngOnInit(): void {
    this.currentDisplayName = this.resolveDisplayName();
    this.loadRooms();
  }

  loadRooms(): void {
    this.roomApi.listRooms().subscribe({
      next: rooms => {
        this.rooms = this.sortNewestFirst(rooms);
        this.ensureValidPage();
        this.error = '';
      },
      error: () => {
        this.error = 'Failed to load rooms.';
      }
    });
  }

  createRoom(): void {
    this.joinError = '';
    const name = this.newRoomName.trim();
    if (!name) {
      return;
    }

    this.roomApi.createRoom(name).subscribe({
      next: room => {
        this.newRoomName = '';
        this.showCreateModal = false;
        this.rooms = this.sortNewestFirst([room, ...this.rooms]);
        this.currentPage = 1;
      },
      error: () => {
        this.error = 'Failed to create room. Try again later.';
      }
    });
  }

  joinRoom(): void {
    this.joinError = '';

    const parsedRoomId = Number(this.joinRoomCodeInput);
    if (!Number.isInteger(parsedRoomId) || parsedRoomId <= 0) {
      this.joinError = 'Enter a valid room ID.';
      return;
    }

    const alreadyMember = this.rooms.some(existing => existing.id === parsedRoomId);
    if (alreadyMember) {
      this.joinError = 'You are already in that room.';
      return;
    }

    this.joining = true;
    this.roomApi.joinRoom(parsedRoomId).subscribe({
      next: room => {
        this.joining = false;
        this.joinRoomCodeInput = '';
        this.showJoinModal = false;

        const exists = this.rooms.some(existing => existing.id === room.id);
        if (!exists) {
          this.rooms = this.sortNewestFirst([room, ...this.rooms]);
        }

        this.openRoom(room.id);
      },
      error: err => {
        this.joining = false;
        if (Number(err?.status) === 404) {
          this.joinError = 'Room not found.';
          return;
        }
        this.joinError = err?.error?.message ?? 'Unable to join room.';
      }
    });
  }

  openRoom(roomId: number): void {
    this.router.navigate(['/rooms', roomId]);
  }

  openCreateModal(): void {
    this.joinError = '';
    this.error = '';
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  openJoinModal(): void {
    this.joinError = '';
    this.error = '';
    this.showJoinModal = true;
  }

  closeJoinModal(): void {
    this.showJoinModal = false;
  }

  onJoinCodeInput(value: string): void {
    this.joinRoomCodeInput = value.replace(/\D/g, '');
  }

  get pagedRooms(): Room[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.rooms.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.rooms.length / this.pageSize));
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage -= 1;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage += 1;
    }
  }

  private ensureValidPage(): void {
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
  }

  private resolveDisplayName(): string {
    try {
      const raw = localStorage.getItem('vsr_user');
      if (!raw) {
        return 'Student';
      }
      const parsed = JSON.parse(raw) as { displayName?: string };
      const displayName = parsed.displayName?.trim();
      return displayName ? displayName : 'Student';
    } catch {
      return 'Student';
    }
  }

  isOwnedByCurrentUser(room: Room): boolean {
    const currentId = this.getCurrentUserId();
    return currentId !== null && room.ownerId === currentId;
  }

  private getCurrentUserId(): number | null {
    try {
      const raw = localStorage.getItem('vsr_user');
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as { userId?: number };
      const id = Number(parsed.userId);
      return Number.isFinite(id) ? id : null;
    } catch {
      return null;
    }
  }

  private sortNewestFirst(rooms: Room[]): Room[] {
    return [...rooms].sort((a, b) => {
      const aTime = new Date(a.createdAt).getTime();
      const bTime = new Date(b.createdAt).getTime();
      if (Number.isNaN(aTime) || Number.isNaN(bTime)) {
        return b.id - a.id;
      }
      return bTime - aTime;
    });
  }
}