import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
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
});
