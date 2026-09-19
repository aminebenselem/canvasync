import { Routes } from '@angular/router';
import { authGuard } from './services/auth-guard';

export const routes: Routes = [
    {
        path: '',
        redirectTo: '/home',
        pathMatch: 'full'
    },
    {
        path: 'login',
        loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent)
    },
    {
        path: 'register',
        loadComponent: () => import('./auth/register/register').then(m => m.Register)
    },
    {
        path: 'home',
        loadComponent: () => import('./features/home/home').then(m => m.Home)
    },
    {
        path: 'whiteboard/:boardId',
        canActivate: [authGuard],
        loadComponent: () => import('./features/whiteboard/canvas/canvas').then(m => m.Canvas)
    },
    {
        path: 'workspace',
        canActivate: [authGuard],
        loadComponent: () => import('./features/workspace/workspace').then(m => m.Workspace)
    }
];
