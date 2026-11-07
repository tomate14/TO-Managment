import { TestBed } from '@angular/core/testing';
import { LoginHero } from './login-hero';

function render(variant?: 'login' | 'register') {
  const fixture = TestBed.createComponent(LoginHero);
  if (variant) {
    fixture.componentRef.setInput('variant', variant);
  }
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('LoginHero', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [LoginHero] }));

  it('defaults to the login variant with motto and metric chips', () => {
    const el = render();
    expect(el.querySelector('.hero--register')).toBeNull();
    expect(el.textContent).toContain('Cada sesión, cada segmento, cada avance.');
    expect(el.querySelectorAll('.hero__chip').length).toBe(4);
  });

  it('the register variant shows the sign-up copy and no chips or login motto', () => {
    const el = render('register');
    expect(el.querySelector('.hero--register')).not.toBeNull();
    expect(el.textContent).toContain('organizado desde el primer día.');
    expect(el.textContent).toContain('Al aprobarse tu cuenta empezás con los catálogos de segmentos, cirugías y cicatrices');
    expect(el.querySelectorAll('.hero__chip').length).toBe(0);
    expect(el.textContent).not.toContain('Cada sesión, cada segmento, cada avance.');
  });
});
