import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AgendaTherapist, Appointment, AppointmentPayload, AvailabilityQuery, Slot } from './agenda.models';

@Injectable({ providedIn: 'root' })
export class AgendaService {
  private readonly http = inject(HttpClient);

  list(from: string, to: string, therapistIds: number[] = []): Observable<Appointment[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (therapistIds.length > 0) {
      params = params.set('therapistIds', therapistIds.join(','));
    }
    return this.http.get<Appointment[]>('/api/appointments', { params });
  }

  availability(query: AvailabilityQuery): Observable<Slot[]> {
    let params = new HttpParams()
      .set('therapistId', query.therapistId)
      .set('date', query.date)
      .set('durationMinutes', query.durationMinutes);
    if (query.patientId !== undefined) {
      params = params.set('patientId', query.patientId);
    }
    if (query.excludeAppointmentId !== undefined) {
      params = params.set('excludeAppointmentId', query.excludeAppointmentId);
    }
    return this.http.get<Slot[]>('/api/appointments/availability', { params });
  }

  get(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`/api/appointments/${id}`);
  }

  book(payload: AppointmentPayload): Observable<Appointment> {
    return this.http.post<Appointment>('/api/appointments', payload);
  }

  reschedule(id: number, payload: AppointmentPayload): Observable<Appointment> {
    return this.http.put<Appointment>(`/api/appointments/${id}`, payload);
  }

  cancel(id: number): Observable<void> {
    return this.http.delete<void>(`/api/appointments/${id}`);
  }

  therapists(): Observable<AgendaTherapist[]> {
    return this.http.get<AgendaTherapist[]>('/api/agenda/therapists');
  }
}
