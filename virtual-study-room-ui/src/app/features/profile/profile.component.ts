/*
renders profile ui and handles profile loading and updates
*/
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProfileApiService } from '../../core/services/profile-api.service';
import { UpdateProfileRequest } from '../../shared/models/profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-[920px]">
      <p *ngIf="loading" class="mb-3 text-sm">Loading profile...</p>
      <p *ngIf="error" class="mb-3 text-sm text-red-700">{{ error }}</p>

      <div *ngIf="!loading" class="border border-zinc-600 bg-zinc-100 p-3 md:p-5">
        <div class="grid gap-4 md:grid-cols-[220px_1fr]">
          <div class="h-[220px] border border-zinc-600 bg-zinc-200">
            <div class="flex h-full items-center justify-center text-7xl font-semibold text-zinc-700">{{ avatarInitial }}</div>
          </div>

          <div>
            <h2 class="mb-3 text-5xl font-normal leading-none">Profile</h2>

            <form [formGroup]="form" (ngSubmit)="submit()" class="grid gap-2">
              <label class="text-[11px] font-semibold uppercase tracking-wide text-zinc-700">Username</label>
              <input type="text" formControlName="displayName" class="h-11 border border-zinc-600 bg-white px-3" />

              <label class="mt-1 text-[11px] font-semibold uppercase tracking-wide text-zinc-700">Email</label>
              <input type="email" formControlName="email" class="h-11 border border-zinc-600 bg-white px-3" />

              <label class="mt-1 text-[11px] font-semibold uppercase tracking-wide text-zinc-700">Password</label>
              <input
                type="password"
                formControlName="newPassword"
                placeholder="Leave blank to keep current password"
                class="h-11 border border-zinc-600 bg-white px-3"
              />

              <label class="mt-1 text-[11px] font-semibold uppercase tracking-wide text-zinc-700">User Bio</label>
              <textarea
                formControlName="bio"
                rows="4"
                placeholder="Share a short study bio"
                class="resize-none border border-zinc-600 bg-white px-3 py-2"
              ></textarea>

              <button
                type="submit"
                [disabled]="form.invalid || saving"
                class="ml-auto mt-2 w-[170px] border border-zinc-600 bg-zinc-100 px-4 py-1.5 text-sm font-semibold hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {{ saving ? 'UPDATING...' : 'UPDATE' }}
              </button>
            </form>

            <p *ngIf="success" class="mt-3 text-sm text-green-800">{{ success }}</p>
          </div>
        </div>
      </div>
    </section>
  `
})
export class ProfileComponent implements OnInit {
  loading = true;
  saving = false;
  error = '';
  success = '';
  private currentEmail = '';

  form;

  constructor(
    private formBuilder: FormBuilder,
    private profileApi: ProfileApiService,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.formBuilder.group({
      displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      bio: ['', [Validators.maxLength(500)]],
      newPassword: ['', [Validators.minLength(8), Validators.maxLength(100)]]
    });
  }

  ngOnInit(): void {
    this.profileApi.getMyProfile().subscribe({
      next: profile => {
        this.loading = false;
        this.currentEmail = profile.email;
        this.form.patchValue({
          displayName: profile.displayName,
          email: profile.email,
          bio: profile.bio ?? '',
          newPassword: ''
        });
      },
      error: () => {
        this.loading = false;
        this.error = 'Failed to load profile.';
      }
    });
  }

  get avatarInitial(): string {
    const raw = this.form.get('displayName')?.value;
    const text = typeof raw === 'string' ? raw.trim() : '';
    return text ? text.charAt(0).toUpperCase() : 'U';
  }

  submit(): void {
    this.success = '';
    this.error = '';
    if (this.form.invalid) {
      return;
    }

    const raw = this.form.getRawValue() as {
      displayName: string;
      email: string;
      bio: string;
      newPassword: string;
    };

    const payload: UpdateProfileRequest = {
      displayName: raw.displayName,
      email: raw.email,
      bio: raw.bio?.trim() ? raw.bio.trim() : '',
      ...(raw.newPassword && raw.newPassword.trim() ? { newPassword: raw.newPassword } : {})
    };

    const passwordChanged = !!raw.newPassword && !!raw.newPassword.trim();

    this.saving = true;
    this.profileApi.updateMyProfile(payload).subscribe({
      next: profile => {
        this.saving = false;

        const emailChanged = profile.email.trim().toLowerCase() !== this.currentEmail.trim().toLowerCase();
        if (emailChanged || passwordChanged) {
          this.authService.logout();
          this.router.navigate(['/login']);
          return;
        }

        this.authService.syncStoredIdentity(profile.email, profile.displayName);
        this.currentEmail = profile.email;
        this.success = 'Profile updated successfully.';
        this.form.patchValue({
          displayName: profile.displayName,
          email: profile.email,
          bio: profile.bio ?? raw.bio,
          newPassword: ''
        });
      },
      error: err => {
        this.saving = false;
        this.error = err?.error?.message ?? 'Failed to update profile.';
      }
    });
  }
}

