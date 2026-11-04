import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';

const APP_NAME = 'Terapia Ocupacional';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: `Ingresar · ${APP_NAME}`,
    loadComponent: () => import('./features/auth/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pacientes' },
      {
        path: 'pacientes',
        title: `Pacientes · ${APP_NAME}`,
        data: { heading: 'Pacientes' },
        loadComponent: () => import('./features/empty-section/empty-section').then((m) => m.EmptySection),
      },
      {
        path: 'plantillas',
        title: `Plantillas · ${APP_NAME}`,
        data: { heading: 'Plantillas' },
        loadComponent: () => import('./features/empty-section/empty-section').then((m) => m.EmptySection),
      },
      {
        path: 'usuarios',
        canActivate: [adminGuard],
        children: [
          {
            path: '',
            title: `Usuarios · ${APP_NAME}`,
            loadComponent: () => import('./features/users/users-page/users-page').then((m) => m.UsersPage),
          },
          {
            path: 'nuevo',
            title: `Nuevo usuario · ${APP_NAME}`,
            loadComponent: () => import('./features/users/user-form-page/user-form-page').then((m) => m.UserFormPage),
          },
          {
            path: ':id',
            title: `Editar usuario · ${APP_NAME}`,
            loadComponent: () => import('./features/users/user-form-page/user-form-page').then((m) => m.UserFormPage),
          },
        ],
      },
      {
        path: 'mas',
        title: `Más · ${APP_NAME}`,
        loadComponent: () => import('./features/more/more-page').then((m) => m.MorePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
