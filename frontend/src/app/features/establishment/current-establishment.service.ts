import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const ESTABLISHMENT_URL = '/api/establishment';

/** The signed-in user's own establishment (ADMIN, or SUPERADMIN inside one). */
export interface CurrentEstablishment {
  id: number;
  name: string;
  activeUsers: number;
  /** ISO yyyy-MM-dd */
  createdDate: string;
}

@Injectable({ providedIn: 'root' })
export class CurrentEstablishmentService {
  private readonly http = inject(HttpClient);

  get(): Observable<CurrentEstablishment> {
    return this.http.get<CurrentEstablishment>(ESTABLISHMENT_URL);
  }

  rename(name: string): Observable<CurrentEstablishment> {
    return this.http.put<CurrentEstablishment>(ESTABLISHMENT_URL, { name });
  }
}
