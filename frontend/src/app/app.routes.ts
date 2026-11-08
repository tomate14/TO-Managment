import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { adminGuard, authGuard, establishmentGuard, guestGuard, superadminGuard } from './core/auth/auth.guards';
import { AuthService } from './core/auth/auth.service';

const APP_NAME = 'Terapia Ocupacional';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: `Ingresar · ${APP_NAME}`,
    loadComponent: () => import('./features/auth/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'registro',
    canActivate: [guestGuard],
    title: `Crear cuenta · ${APP_NAME}`,
    loadComponent: () => import('./features/auth/register-page/register-page').then((m) => m.RegisterPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => {
          const auth = inject(AuthService);
          return auth.isSuperadmin() && !auth.establishment() ? 'establecimientos' : 'pacientes';
        },
      },
      {
        path: 'establecimientos',
        canActivate: [superadminGuard],
        title: `Establecimientos · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/establishments/establishments-page/establishments-page').then((m) => m.EstablishmentsPage),
      },
      {
        path: 'establecimientos/nuevo',
        canActivate: [superadminGuard],
        title: `Nuevo establecimiento · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/establishments/establishment-form-page/establishment-form-page').then(
            (m) => m.EstablishmentFormPage,
          ),
      },
      {
        path: 'establecimiento',
        canActivate: [establishmentGuard, adminGuard],
        title: `Establecimiento · ${APP_NAME}`,
        loadComponent: () =>
          import('./features/establishment/establishment-page/establishment-page').then((m) => m.EstablishmentPage),
      },
      {
        path: 'pacientes',
        canActivate: [establishmentGuard],
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
          {
            path: ':id/diagnosticos/nuevo',
            title: `Nuevo diagnóstico · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/diagnoses/diagnosis-form-page/diagnosis-form-page').then((m) => m.DiagnosisFormPage),
          },
          {
            path: ':id/diagnosticos/:diagnosisId',
            title: `Diagnóstico · ${APP_NAME}`,
            loadComponent: () => import('./features/diagnoses/diagnosis-page/diagnosis-page').then((m) => m.DiagnosisPage),
          },
          {
            path: ':id/diagnosticos/:diagnosisId/seguimientos/nuevo',
            title: `Nuevo seguimiento · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/follow-ups/follow-up-form-page/follow-up-form-page').then((m) => m.FollowUpFormPage),
          },
          {
            path: ':id/diagnosticos/:diagnosisId/facturacion/nueva',
            title: `Registrar facturación · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/billing/billing-form-page/billing-form-page').then((m) => m.BillingFormPage),
          },
        ],
      },
      {
        path: 'agenda',
        canActivate: [establishmentGuard],
        children: [
          {
            path: '',
            title: `Agenda · ${APP_NAME}`,
            loadComponent: () => import('./features/agenda/agenda-page/agenda-page').then((m) => m.AgendaPage),
          },
          {
            path: 'nuevo',
            title: `Nuevo turno · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/agenda/appointment-form-page/appointment-form-page').then((m) => m.AppointmentFormPage),
          },
          {
            path: ':id/reprogramar',
            title: `Reprogramar turno · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/agenda/appointment-form-page/appointment-form-page').then((m) => m.AppointmentFormPage),
          },
        ],
      },
      {
        path: 'plantillas',
        canActivate: [establishmentGuard],
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
        canActivate: [establishmentGuard, adminGuard],
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
        canActivate: [establishmentGuard, adminGuard],
        children: [
          {
            path: '',
            title: `Catálogos · ${APP_NAME}`,
            loadComponent: () => import('./features/catalogs/catalogs-page/catalogs-page').then((m) => m.CatalogsPage),
          },
          {
            path: 'segmentos/:segmentId',
            title: `Métricas ROM · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/segment-metrics/segment-metrics-page/segment-metrics-page').then((m) => m.SegmentMetricsPage),
          },
          {
            path: 'segmentos/:segmentId/metricas/nueva',
            title: `Nueva métrica · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/segment-metrics/segment-metric-form-page/segment-metric-form-page').then(
                (m) => m.SegmentMetricFormPage,
              ),
          },
          {
            path: 'segmentos/:segmentId/metricas/:metricId',
            title: `Editar métrica · ${APP_NAME}`,
            loadComponent: () =>
              import('./features/segment-metrics/segment-metric-form-page/segment-metric-form-page').then(
                (m) => m.SegmentMetricFormPage,
              ),
          },
        ],
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
