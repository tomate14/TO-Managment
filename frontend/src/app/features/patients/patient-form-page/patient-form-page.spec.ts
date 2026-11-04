import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PatientFormPage } from './patient-form-page';
import { PatientsService } from '../patients.service';
import { Patient } from '../patients.models';

describe('PatientFormPage', () => {
  const lucia: Patient = {
    id: 7, dni: '30123456', firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '249 412-3456',
    occupation: 'Docente', healthInsurance: 'OSDE', healthInsuranceNumber: '61 234567 8 01', dominance: 'RIGHT',
  };
  let service: Record<'get' | 'create' | 'update', ReturnType<typeof vi.fn>>;

  function setup(id: string | null) {
    service = { get: vi.fn(() => of(lucia)), create: vi.fn(() => of(lucia)), update: vi.fn(() => of(lucia)) };
    TestBed.configureTestingModule({
      imports: [PatientFormPage],
      providers: [
        provideRouter([]),
        { provide: PatientsService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(PatientFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    const fillAll = () => {
      type('#patient-dni', '30.123.456');
      type('#patient-first-name', ' Lucía ');
      type('#patient-last-name', 'Fernández');
      type('#patient-birth-date', '1984-03-21');
      type('#patient-phone', '249 412-3456');
      type('#patient-occupation', 'Docente');
      type('#patient-insurance', 'OSDE');
      type('#patient-insurance-number', '61 234567 8 01');
    };
    return { fixture, el, navigate, type, submit, fillAll };
  }

  it('shows the mockup sections and Diestro preselected', () => {
    const { el } = setup(null);
    expect(el.querySelector('h1')?.textContent).toContain('Nuevo paciente');
    expect(el.textContent).toContain('Datos personales');
    expect(el.textContent).toContain('Cobertura');
    expect(el.querySelector('.segmented__option[aria-pressed="true"]')?.textContent).toContain('Diestro');
    expect(el.textContent).toContain('Todos los campos son obligatorios.');
  });

  it('requires every field', () => {
    const { el, submit } = setup(null);
    submit();
    expect(service.create).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Ingresá el DNI.');
    expect(el.textContent).toContain('Ingresá la fecha de nacimiento.');
    expect(el.textContent).toContain('Ingresá el número de afiliado.');
  });

  it('rejects a future birth date and a malformed DNI', () => {
    const { el, type, submit, fillAll } = setup(null);
    fillAll();
    type('#patient-dni', '12');
    type('#patient-birth-date', '2999-01-01');
    submit();
    expect(service.create).not.toHaveBeenCalled();
    expect(el.textContent).toContain('El DNI tiene que tener entre 6 y 9 números.');
    expect(el.textContent).toContain('La fecha de nacimiento no puede ser futura.');
  });

  it('creates the patient with the DNI as digits and opens the patient page', () => {
    const { el, navigate, submit, fillAll } = setup(null);
    fillAll();
    Array.from(el.querySelectorAll<HTMLButtonElement>('.segmented__option')).find((b) => b.textContent?.includes('Zurdo'))!.click();
    submit();

    expect(service.create).toHaveBeenCalledWith({
      dni: '30123456', firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '249 412-3456',
      occupation: 'Docente', healthInsurance: 'OSDE', healthInsuranceNumber: '61 234567 8 01', dominance: 'LEFT',
    });
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7], { replaceUrl: true });
  });

  it('shows "Ya existe un paciente con este DNI." under the DNI', () => {
    const { el, submit, fillAll } = setup(null);
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { code: 'PATIENT_DNI_DUPLICATE', message: 'Ya existe un paciente con este DNI.' },
    })));
    fillAll();
    submit();
    expect(el.querySelector('#patient-dni-error')?.textContent).toContain('Ya existe un paciente con este DNI.');
    expect(el.querySelector('#patient-dni')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('edits everything but the DNI', () => {
    const { el, navigate, type, submit } = setup('7');
    expect(el.querySelector('h1')?.textContent).toContain('Editar paciente');
    expect(el.querySelector<HTMLInputElement>('#patient-dni')?.disabled).toBe(true);
    expect(el.querySelector<HTMLInputElement>('#patient-first-name')?.value).toBe('Lucía');

    type('#patient-occupation', 'Directora');
    submit();

    expect(service.update).toHaveBeenCalledWith(7, expect.objectContaining({ occupation: 'Directora' }));
    expect(service.update.mock.calls[0][1]).not.toHaveProperty('dni');
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7], { replaceUrl: true });
  });
});
