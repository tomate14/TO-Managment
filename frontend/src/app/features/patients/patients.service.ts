import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatePatientPayload, PATIENT_PAGE_SIZE, PageResponse, Patient, PatientData } from './patients.models';

const PATIENTS_URL = '/api/patients';

@Injectable({ providedIn: 'root' })
export class PatientsService {
  private readonly http = inject(HttpClient);

  /** @param page zero-based */
  search(query: string, page: number, size = PATIENT_PAGE_SIZE): Observable<PageResponse<Patient>> {
    let params = new HttpParams().set('page', page).set('size', size);
    const text = query.trim();
    if (text !== '') {
      params = params.set('q', text);
    }
    return this.http.get<PageResponse<Patient>>(PATIENTS_URL, { params });
  }

  get(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${PATIENTS_URL}/${id}`);
  }

  create(payload: CreatePatientPayload): Observable<Patient> {
    return this.http.post<Patient>(PATIENTS_URL, payload);
  }

  update(id: number, payload: PatientData): Observable<Patient> {
    return this.http.put<Patient>(`${PATIENTS_URL}/${id}`, payload);
  }
}
