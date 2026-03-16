/*
declares route mappings and guard rules for frontend navigation
*/
import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProfileComponent } from './features/profile/profile.component';
import { StudyRoomComponent } from './features/study-room/study-room.component';
import { authGuard } from './core/guards/auth.guard';
import { publicOnlyGuard } from './core/guards/public-only.guard';

export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'dashboard' },
	{ path: 'login', component: LoginComponent, canActivate: [publicOnlyGuard] },
	{ path: 'register', component: RegisterComponent, canActivate: [publicOnlyGuard] },
	{ path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
	{ path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
	{ path: 'rooms/:roomId', component: StudyRoomComponent, canActivate: [authGuard] },
	{ path: '**', redirectTo: 'dashboard' }
];


