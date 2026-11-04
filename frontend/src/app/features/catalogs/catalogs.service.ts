import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CatalogItem, CatalogSlug } from './catalogs.models';

const CATALOGS_URL = '/api/catalogs';

@Injectable({ providedIn: 'root' })
export class CatalogsService {
  private readonly http = inject(HttpClient);

  list(slug: CatalogSlug, activeOnly = false): Observable<CatalogItem[]> {
    const params = activeOnly ? new HttpParams().set('activeOnly', 'true') : undefined;
    return this.http.get<CatalogItem[]>(`${CATALOGS_URL}/${slug}`, { params });
  }

  create(slug: CatalogSlug, name: string): Observable<CatalogItem> {
    return this.http.post<CatalogItem>(`${CATALOGS_URL}/${slug}`, { name });
  }

  rename(slug: CatalogSlug, id: number, name: string): Observable<CatalogItem> {
    return this.http.put<CatalogItem>(`${CATALOGS_URL}/${slug}/${id}`, { name });
  }

  setActive(slug: CatalogSlug, id: number, active: boolean): Observable<CatalogItem> {
    return this.http.patch<CatalogItem>(`${CATALOGS_URL}/${slug}/${id}/active`, { active });
  }
}
