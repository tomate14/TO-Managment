import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TemplatesService } from './templates.service';
import { QuestionPayload } from './templates.models';

describe('TemplatesService', () => {
  let service: TemplatesService;
  let http: HttpTestingController;
  const question: QuestionPayload = {
    text: '¿Usa férula?', answerType: 'YES_NO', minValue: null, maxValue: null, options: [], parentQuestionId: null, condition: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TemplatesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists, gets, creates, updates and toggles templates', () => {
    service.list().subscribe();
    expect(http.expectOne('/api/templates').request.method).toBe('GET');
    service.get(3).subscribe();
    expect(http.expectOne('/api/templates/3').request.method).toBe('GET');

    service.create({ name: 'Fractura de muñeca', segmentId: 1 }).subscribe();
    const create = http.expectOne('/api/templates');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({ name: 'Fractura de muñeca', segmentId: 1 });

    service.update(3, { name: 'Otra', segmentId: 2 }).subscribe();
    expect(http.expectOne('/api/templates/3').request.method).toBe('PUT');
    service.setActive(3, false).subscribe();
    const toggle = http.expectOne('/api/templates/3/active');
    expect(toggle.request.method).toBe('PATCH');
    expect(toggle.request.body).toEqual({ active: false });
  });

  it('adds, updates, toggles and deletes questions', () => {
    service.addQuestion(3, question).subscribe();
    const add = http.expectOne('/api/templates/3/questions');
    expect(add.request.method).toBe('POST');
    expect(add.request.body).toEqual(question);

    service.updateQuestion(3, 9, question).subscribe();
    expect(http.expectOne('/api/templates/3/questions/9').request.method).toBe('PUT');
    service.setQuestionActive(3, 9, false).subscribe();
    expect(http.expectOne('/api/templates/3/questions/9/active').request.body).toEqual({ active: false });
    service.deleteQuestion(3, 9).subscribe();
    expect(http.expectOne('/api/templates/3/questions/9').request.method).toBe('DELETE');
  });
});
