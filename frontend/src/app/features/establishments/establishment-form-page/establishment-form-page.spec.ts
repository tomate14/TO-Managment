import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { EstablishmentFormPage } from './establishment-form-page';
import { EstablishmentsService } from '../establishments.service';

describe('EstablishmentFormPage', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EstablishmentFormPage],
      providers: [provideRouter([]), { provide: EstablishmentsService, useValue: { create: vi.fn() } }],
    });
  });

  it('shows the form under a "Nuevo establecimiento" header', () => {
    const fixture = TestBed.createComponent(EstablishmentFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toContain('Nuevo establecimiento');
    expect(el.querySelector('app-establishment-form')).toBeTruthy();
  });

  it('returns to the list when cancelled', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(EstablishmentFormPage);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.est-form__cancel')!.click();
    expect(navigate).toHaveBeenCalledWith('/establecimientos');
  });
});
