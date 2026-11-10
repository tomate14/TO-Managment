import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { appointmentOf } from '../agenda-test-data';
import { AgendaService } from '../agenda.service';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { PatientsService } from '../../patients/patients.service';
import { AppointmentFormPage } from './appointment-form-page';

describe('AppointmentFormPage confirmed appointment', () => {
  it('shows the confirmed card with only Ver ficha when a reschedule URL is opened directly', () => {
    TestBed.configureTestingModule({
      imports: [AppointmentFormPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }), queryParamMap: convertToParamMap({}) } } },
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: {
          therapists: () => of([]),
          get: () => of({ ...appointmentOf({ id: 1 }), confirmedAt: '2026-11-05T12:00:00Z', billed: false }),
          availability: () => of([]),
        } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, fullName: 'Ana', role: 'TERAPEUTA' }) } },
        { provide: DiagnosesService, useValue: { listForPatient: () => of([]) } },
        { provide: PatientsService, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(AppointmentFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('app-appointment-form')).toBeNull();
    expect(el.textContent).toContain('Confirmado · Pendiente de facturar');
    expect(el.querySelector('.appointment-card__actions')?.textContent?.trim()).toBe('Ver ficha');
  });
});
