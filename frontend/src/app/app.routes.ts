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
        children: [
          {
            path: '',
            title: `Pacientes · ${APP_NAME}`,
            loadComponent: () => import('./features/patients/patients-page/patients-page').then((m) => m.PatientsPage),
          },
          {
            path: 'nuevo',
            title: `Nuevo paciente · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/patients/patient-form-page/patient-form-page').then((m) => m.PatientFormPage),
          },
          {
            path: ':id',
            title: `Ficha del paciente · ${APP_NAME}`,
            loadComponent: () => import('./features/patients/patient-page/patient-page').then((m) => m.PatientPage),
          },
          {
            path: ':id/editar',
            title: `Editar paciente · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/patients/patient-form-page/patient-form-page').then((m) => m.PatientFormPage),
          },
        ],
      },
      {
        path: 'plantillas',
        children: [
          {
            path: '',
            title: `Plantillas · ${APP_NAME}`,
            loadComponent: () => import('./features/templates/templates-page/templates-page').then((m) => m.TemplatesPage),
          },
          {
            path: 'nueva',
            title: `Nueva plantilla · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/templates/template-edit-page/template-edit-page').then((m) => m.TemplateEditPage),
          },
          {
            path: ':id',
            title: `Editar plantilla · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/templates/template-edit-page/template-edit-page').then((m) => m.TemplateEditPage),
          },
          {
            path: ':id/preguntas/nueva',
            title: `Nueva pregunta · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/templates/question-form-page/question-form-page').then((m) => m.QuestionFormPage),
          },
          {
            path: ':id/preguntas/:questionId',
            title: `Editar pregunta · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/templates/question-form-page/question-form-page').then((m) => m.QuestionFormPage),
          },
        ],
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
        path: 'catalogos',
        canActivate: [adminGuard],
        title: `Catálogos · ${APP_NAME}`,
        loadComponent: () => import('./features/catalogs/catalogs-page/catalogs-page').then((m) => m.CatalogsPage),
      },
      {
        path: 'cambiar-contrasena',
        title: `Cambiar contraseña · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/account/change-password-page/change-password-page').then((m) => m.ChangePasswordPage),
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
