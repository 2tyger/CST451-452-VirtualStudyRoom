import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-[760px] border border-zinc-600 bg-zinc-100 px-6 py-10 md:px-10">
      <h2 class="mb-10 text-center text-5xl font-normal">Join the Study Club!</h2>

      <form [formGroup]="form" (ngSubmit)="submit()" class="mx-auto grid max-w-[460px] gap-3">
        <label class="text-xs font-semibold uppercase tracking-wide text-zinc-700">Email</label>
        <input type="email" formControlName="email" class="h-11 border border-zinc-600 bg-white px-3" />

        <label class="mt-1 text-xs font-semibold uppercase tracking-wide text-zinc-700">Password</label>
        <input type="password" formControlName="password" class="h-11 border border-zinc-600 bg-white px-3" />

        <button
          type="submit"
          [disabled]="form.invalid || loading"
          class="mx-auto mt-3 w-[170px] rounded-full border border-zinc-600 bg-zinc-100 px-4 py-1.5 text-sm font-semibold hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {{ loading ? 'SIGNING IN...' : 'LOG IN' }}
        </button>
      </form>

      <p *ngIf="error" class="mx-auto mt-4 max-w-[460px] text-sm text-red-700">{{ error }}</p>

      <div class="mt-3 text-center text-sm">
        <span>Don't have an account? </span><a routerLink="/register" class="font-semibold underline">Sign up!</a>
      </div>
    </section>
  `
})
export class LoginComponent {
  loading = false;
  error = '';
  form;

  constructor(private formBuilder: FormBuilder, private authService: AuthService, private router: Router) {
    this.form = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.error = '';

    this.authService.login(this.form.getRawValue() as { email: string; password: string }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.loading = false;
        this.error = 'Login failed. Check credentials.';
      }
    });
  }
}