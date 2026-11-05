import { vi } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PatientForm } from './patient-form';
import { PatientsService } from '../patients.service';
import { Patient } from '../patients.models';

describe('PatientForm', () => {
  const lucia: Patient = {
    id: 7, dni: '30123456', firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '249 412-3456',
    occupation: 'Docente', healthInsurance: 'OSDE', healthInsuranceNumber: '61 234567 8 01', dominance: 'RIGHT',
  };
  let create: ReturnType<typeof vi.fn>;

  function setup(initialDni = '30123456') {
    create = vi.fn(() => of(lucia));
    TestBed.configureTestingModule({ providers: [{ provide: PatientsService, useValue: { create } }] });
    const fixture = TestBed.createComponent(PatientForm);
    fixture.componentRef.setInput('initialDni', initialDni);
    const saved: Patient[] = [];
    let cancelled = 0;
    fixture.componentInstance.saved.subscribe((patient) => saved.push(patient));
    fixture.componentInstance.cancelled.subscribe(() => (cancelled += 1));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const fillRest = () => {
      type('#patient-first-name', 'Lucía');
      type('#patient-last-name', 'Fernández');
      type('#patient-birth-date', '1984-03-21');
      type('#patient-phone', '249 412-3456');
      type('#patient-occupation', 'Docente');
      type('#patient-insurance', 'OSDE');
      type('#patient-insurance-number', '61 234567 8 01');
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { fixture, el, saved, cancelledCount: () => cancelled, type, fillRest, submit };
  }

  it('prefills the DNI and uses the default labels', () => {
    const { el } = setup('30123456');
    expect(el.querySelector<HTMLInputElement>('#patient-dni')?.value).toBe('30123456');
    expect(el.querySelector<HTMLInputElement>('#patient-dni')?.disabled).toBe(false);
    expect(el.querySelector('button[type="submit"]')?.textContent).toContain('Guardar paciente');
  });

  it('emits the created patient', () => {
    const { el, saved, fillRest, submit } = setup();
    fillRest();
    submit();
    expect(create).toHaveBeenCalledWith(expect.objectContaining({ dni: '30123456', firstName: 'Lucía', dominance: 'RIGHT' }));
    expect(saved).toEqual([lucia]);
    expect(el.querySelector('form')).not.toBeNull();
  });

  it('keeps the form and shows the message when the DNI already exists', () => {
    const { el, saved, fillRest, submit } = setup();
    create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { code: 'PATIENT_DNI_DUPLICATE', message: 'Ya existe un paciente con este DNI.' },
    })));
    fillRest();
    submit();
    expect(saved).toEqual([]);
    expect(el.querySelector('#patient-dni-error')?.textContent).toContain('Ya existe un paciente con este DNI.');
    expect(el.querySelector<HTMLInputElement>('#patient-first-name')?.value).toBe('Lucía');
  });

  it('emits cancelled from the cancel button', () => {
    const { el, cancelledCount } = setup();
    const cancel = Array.from(el.querySelectorAll('button')).find((button) => button.type === 'button' && button.textContent?.trim() === 'Cancelar');
    cancel!.click();
    expect(cancelledCount()).toBe(1);
  });
});
