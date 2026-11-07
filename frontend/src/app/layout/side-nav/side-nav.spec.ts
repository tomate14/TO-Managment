import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { SideNav } from './side-nav';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/auth/auth.models';
import { EstablishmentsService } from '../../features/establishments/establishments.service';

describe('SideNav', () => {
  const superadmin: UserSummary = { id: 1, email: 'm@c.com', fullName: 'Maximiliano Roselli', role: 'SUPERADMIN', establishment: null };
  const pendingCount = signal(0);

  beforeEach(() => {
    pendingCount.set(0);
    TestBed.configureTestingModule({
      imports: [SideNav],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: signal(superadmin), logout: () => undefined } },
        { provide: EstablishmentsService, useValue: { pendingCount } },
      ],
    });
  });

  function link(el: HTMLElement): HTMLElement {
    return el.querySelector<HTMLElement>('a[href="/establecimientos"]')!;
  }

  it('shows the pending count next to Establecimientos', () => {
    pendingCount.set(2);
    const fixture = TestBed.createComponent(SideNav);
    fixture.detectChanges();
    const badge = link(fixture.nativeElement).querySelector('.side-nav__badge');
    expect(badge?.textContent?.trim()).toBe('2');
  });

  it('shows no badge when nothing is pending', () => {
    const fixture = TestBed.createComponent(SideNav);
    fixture.detectChanges();
    expect(link(fixture.nativeElement).querySelector('.side-nav__badge')).toBeNull();
  });
});
