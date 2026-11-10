import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RedirectFunction, Router, UrlTree, provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { BillingFormPage } from './features/billing/billing-form-page/billing-form-page';

describe('patient billing routes', () => {
  const patientRoutes = routes.find((route) => route.path === '')!.children!.find((route) => route.path === 'pacientes')!.children!;

  it('registers a patient billing form independent of diagnosis', async () => {
    const route = patientRoutes.find((entry) => entry.path === ':id/facturacion/nueva');
    expect(route?.loadComponent).toBeDefined();
    expect(await route!.loadComponent!()).toBe(BillingFormPage);
  });

  it.each(['17', '301'])('redirects the legacy diagnosis form preserving patient %s in a UrlTree', (id) => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const route = patientRoutes.find((entry) => entry.path === ':id/diagnosticos/:diagnosisId/facturacion/nueva')!;
    expect(typeof route.redirectTo).toBe('function');
    const result = TestBed.runInInjectionContext(() => (route.redirectTo as RedirectFunction)({ params: { id, diagnosisId: '999' } } as unknown as ActivatedRouteSnapshot));
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(`/pacientes/${id}/facturacion/nueva`);
  });
});
