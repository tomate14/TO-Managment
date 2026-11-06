import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EstablishmentBanner } from './establishment-banner';
import { AuthService } from '../../core/auth/auth.service';

describe('EstablishmentBanner', () => {
  let enterEstablishment: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    enterEstablishment = vi.fn(() => of(undefined));
    TestBed.configureTestingModule({
      imports: [EstablishmentBanner],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { establishment: signal({ id: 3, name: 'Norte' }), enterEstablishment } },
      ],
    });
  });

  it('shows the banner and leaves the establishment', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(EstablishmentBanner);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Estás en Norte');
    expect(el.textContent).toContain('Salir');

    el.querySelector<HTMLButtonElement>('button')!.click();

    expect(enterEstablishment).toHaveBeenCalledWith(null);
    expect(navigate).toHaveBeenCalledWith(['/establecimientos']);
  });

  it('does not navigate, re-enables the button and shows a message when leaving fails', () => {
    enterEstablishment.mockReturnValue(throwError(() => new Error('boom')));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(EstablishmentBanner);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    el.querySelector<HTMLButtonElement>('button')!.click();
    fixture.detectChanges();

    expect(navigate).not.toHaveBeenCalled();
    expect(el.textContent).toContain('No pudimos salir del establecimiento. Intentá de nuevo.');
    expect(el.querySelector<HTMLButtonElement>('button')!.disabled).toBe(false);
  });
});
