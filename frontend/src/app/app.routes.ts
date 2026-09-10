import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: '',
        redirectTo: '/home',
        pathMatch: 'full'
    } ,
    {
        path: 'home',
        loadComponent: () => import('./features/home/home').then(m => m.Home)        
    },
    {
        path: 'whiteboard',
        loadComponent: () => import('./features/whiteboard/canvas/canvas').then(m => m.Canvas)
    }
];
