import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CurrentEstablishmentService } from './current-establishment.service';

describe('CurrentEstablishmentService', () => {
  let service: CurrentEstablishmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CurrentEstablishmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gets the current establishment', () => {
    service.get().subscribe();
    expect(http.expectOne('/api/establishment').request.method).toBe('GET');
  });

  it('renames the current establishment', () => {
    service.rename('Consultorio Sur').subscribe();
    const req = http.expectOne('/api/establishment');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ name: 'Consultorio Sur' });
  });
});
