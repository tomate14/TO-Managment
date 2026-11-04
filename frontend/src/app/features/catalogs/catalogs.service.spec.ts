import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CatalogsService } from './catalogs.service';

describe('CatalogsService', () => {
  let service: CatalogsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CatalogsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists a whole catalog', () => {
    service.list('body-segments').subscribe();
    const req = http.expectOne('/api/catalogs/body-segments');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('activeOnly')).toBe(false);
  });

  it('lists only active items for pickers', () => {
    service.list('scar-types', true).subscribe();
    expect(http.expectOne('/api/catalogs/scar-types?activeOnly=true').request.method).toBe('GET');
  });

  it('creates, renames and toggles items', () => {
    service.create('surgery-types', 'Artrodesis').subscribe();
    const create = http.expectOne('/api/catalogs/surgery-types');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({ name: 'Artrodesis' });

    service.rename('surgery-types', 4, 'Artrodesis de muñeca').subscribe();
    const rename = http.expectOne('/api/catalogs/surgery-types/4');
    expect(rename.request.method).toBe('PUT');
    expect(rename.request.body).toEqual({ name: 'Artrodesis de muñeca' });

    service.setActive('surgery-types', 4, false).subscribe();
    const toggle = http.expectOne('/api/catalogs/surgery-types/4/active');
    expect(toggle.request.method).toBe('PATCH');
    expect(toggle.request.body).toEqual({ active: false });
  });
});
