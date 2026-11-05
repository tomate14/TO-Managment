import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { QuestionPayload, TemplateDetail, TemplatePayload, TemplateSummary } from './templates.models';

const TEMPLATES_URL = '/api/templates';

@Injectable({ providedIn: 'root' })
export class TemplatesService {
  private readonly http = inject(HttpClient);

  list(): Observable<TemplateSummary[]> {
    return this.http.get<TemplateSummary[]>(TEMPLATES_URL);
  }

  /** Templates of one segment (active and inactive). */
  listForSegment(segmentId: number): Observable<TemplateSummary[]> {
    return this.http.get<TemplateSummary[]>(TEMPLATES_URL, { params: new HttpParams().set('segmentId', segmentId) });
  }

  get(id: number): Observable<TemplateDetail> {
    return this.http.get<TemplateDetail>(`${TEMPLATES_URL}/${id}`);
  }

  create(payload: TemplatePayload): Observable<TemplateDetail> {
    return this.http.post<TemplateDetail>(TEMPLATES_URL, payload);
  }

  update(id: number, payload: TemplatePayload): Observable<TemplateDetail> {
    return this.http.put<TemplateDetail>(`${TEMPLATES_URL}/${id}`, payload);
  }

  setActive(id: number, active: boolean): Observable<TemplateDetail> {
    return this.http.patch<TemplateDetail>(`${TEMPLATES_URL}/${id}/active`, { active });
  }

  addQuestion(templateId: number, payload: QuestionPayload): Observable<TemplateDetail> {
    return this.http.post<TemplateDetail>(`${TEMPLATES_URL}/${templateId}/questions`, payload);
  }

  updateQuestion(templateId: number, questionId: number, payload: QuestionPayload): Observable<TemplateDetail> {
    return this.http.put<TemplateDetail>(`${TEMPLATES_URL}/${templateId}/questions/${questionId}`, payload);
  }

  setQuestionActive(templateId: number, questionId: number, active: boolean): Observable<TemplateDetail> {
    return this.http.patch<TemplateDetail>(`${TEMPLATES_URL}/${templateId}/questions/${questionId}/active`, { active });
  }

  deleteQuestion(templateId: number, questionId: number): Observable<void> {
    return this.http.delete<void>(`${TEMPLATES_URL}/${templateId}/questions/${questionId}`);
  }
}
