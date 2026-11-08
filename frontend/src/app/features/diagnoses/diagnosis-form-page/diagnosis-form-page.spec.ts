import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DiagnosisFormPage } from './diagnosis-form-page';
import { DiagnosesService } from '../diagnoses.service';
import { PatientsService } from '../../patients/patients.service';
import { CatalogsService } from '../../catalogs/catalogs.service';

describe('DiagnosisFormPage', () => {
  const lucia = {
    id: 7, dni: '30123456', firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '1',
    occupation: 'Docente', healthInsurance: 'OSDE', healthInsuranceNumber: '1', dominance: 'RIGHT',
  };
  let create: ReturnType<typeof vi.fn>;
  let catalogList: ReturnType<typeof vi.fn>;

  function setup() {
    create = vi.fn(() => of({ id: 3 }));
    catalogList = vi.fn(() => of([{ id: 1, name: 'Osteosíntesis con placa', active: true }, { id: 2, name: 'Tenorrafia', active: true }]));
    TestBed.configureTestingModule({
      imports: [DiagnosisFormPage],
      providers: [
        provideRouter([]),
        { provide: DiagnosesService, useValue: { create } },
        { provide: PatientsService, useValue: { get: vi.fn(() => of(lucia)) } },
        { provide: CatalogsService, useValue: { list: catalogList } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(DiagnosisFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    };
    const choose = (selector: string, index: number) => {
      const select = el.querySelector<HTMLSelectElement>(selector)!;
      select.value = select.options[index].value;
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    };
    const pick = (label: string) => {
      Array.from(el.querySelectorAll<HTMLButtonElement>('.treatment-option')).find((b) => b.textContent?.includes(label))!.click();
      fixture.detectChanges();
    };
    const answer = (label: string) => {
      Array.from(el.querySelectorAll<HTMLButtonElement>('.segmented__option')).find((b) => b.textContent?.trim() === label)!.click();
      fixture.detectChanges();
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { el, navigate, type, choose, pick, answer, submit };
  }

  it('shows the patient, the automatic date note and surgical data by default', () => {
    const { el } = setup();
    expect(el.textContent).toContain('Lucía Fernández · DNI 30.123.456');
    expect(el.textContent).toContain('La fecha del diagnóstico se registra automáticamente al guardar.');
    expect(el.querySelector('.treatment-option[aria-pressed="true"]')?.textContent).toContain('Quirúrgico');
    expect(el.textContent).toContain('Datos de la cirugía');
    expect(catalogList).toHaveBeenCalledWith('surgery-types', true);
    expect(el.textContent).toContain('El tipo de tratamiento no se puede cambiar');
  });

  it('creates a surgical diagnosis and opens it', () => {
    const { navigate, type, choose, submit } = setup();
    type('#diagnosis-description', ' Fractura distal de radio izquierdo ');
    type('#surgery-date', '2026-10-14');
    type('#stitches-date', '2026-10-28');
    choose('#surgery-type', 1);
    submit();

    expect(create).toHaveBeenCalledWith(7, {
      description: 'Fractura distal de radio izquierdo',
      treatment: { type: 'SURGICAL', surgeryDate: '2026-10-14', stitchesRemovalDate: '2026-10-28', surgeryTypeId: 1, immobilizationDate: null, hasImmobilization: null },
    });
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7, 'diagnosticos', 3], { replaceUrl: true });
  });

  it('rejects a stitches removal before the surgery', () => {
    const { el, type, choose, submit } = setup();
    type('#diagnosis-description', 'Fractura');
    type('#surgery-date', '2026-10-14');
    type('#stitches-date', '2026-10-13');
    choose('#surgery-type', 1);
    submit();
    expect(create).not.toHaveBeenCalled();
    expect(el.querySelector('.field__error')?.textContent).toContain('El retiro de puntos no puede ser anterior a la cirugía.');
  });

  it('switches to conservative and asks whether it has immobilization', () => {
    const { el, type, pick, submit } = setup();
    pick('Conservador');
    expect(el.textContent).not.toContain('Datos de la cirugía');
    expect(el.textContent).toContain('¿Lleva inmovilización?');
    expect(el.querySelector('#immobilization-date')).toBeNull();
    type('#diagnosis-description', 'Tenosinovitis de De Quervain');
    submit();
    expect(el.textContent).toContain('Indicá si lleva inmovilización.');
    expect(create).not.toHaveBeenCalled();
  });

  it('with Sí requires the immobilization date and sends it', () => {
    const { el, type, pick, answer, submit } = setup();
    pick('Conservador');
    type('#diagnosis-description', 'Tenosinovitis de De Quervain');
    answer('Sí');
    expect(el.textContent).toContain('Fecha de inmovilización');
    submit();
    expect(el.textContent).toContain('Ingresá la fecha de inmovilización.');
    expect(create).not.toHaveBeenCalled();

    type('#immobilization-date', '2026-08-02');
    submit();
    expect(create).toHaveBeenCalledWith(7, {
      description: 'Tenosinovitis de De Quervain',
      treatment: {
        type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryTypeId: null,
        immobilizationDate: '2026-08-02', hasImmobilization: true,
      },
    });
  });

  it('with No hides the date and sends no date, even if one was typed before', () => {
    const { el, type, pick, answer, submit } = setup();
    pick('Conservador');
    type('#diagnosis-description', 'Tenosinovitis de De Quervain');
    answer('Sí');
    type('#immobilization-date', '2026-08-02');
    answer('No');
    expect(el.querySelector('#immobilization-date')).toBeNull();
    expect(el.textContent).toContain('Sin inmovilización: no se pide fecha.');
    submit();
    expect(create).toHaveBeenCalledWith(7, {
      description: 'Tenosinovitis de De Quervain',
      treatment: {
        type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryTypeId: null,
        immobilizationDate: null, hasImmobilization: false,
      },
    });
  });

  it('requires a description', () => {
    const { el, submit } = setup();
    submit();
    expect(el.textContent).toContain('Ingresá la descripción del diagnóstico.');
  });

  it('shows backend errors', () => {
    const { el, type, choose, submit } = setup();
    create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'SURGERY_TYPE_INACTIVE', message: 'Ese tipo de cirugía está inactivo: elegí uno activo.' },
    })));
    type('#diagnosis-description', 'Fractura');
    type('#surgery-date', '2026-10-14');
    type('#stitches-date', '2026-10-28');
    choose('#surgery-type', 2);
    submit();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Ese tipo de cirugía está inactivo');
  });
});
