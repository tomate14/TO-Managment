import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { CATALOG_ERROR_CODES, CATALOGS, CatalogDefinition, CatalogItem } from '../catalogs.models';
import { CatalogsService } from '../catalogs.service';

const DUPLICATE_MESSAGE = 'Ya existe con ese nombre (sin importar mayúsculas).';
const BLANK_MESSAGE = 'Ingresá un nombre.';
const LOAD_ERROR = 'No pudimos cargar el catálogo. Intentá de nuevo en unos minutos.';
const SAVE_ERROR = 'No pudimos guardar el cambio. Intentá de nuevo.';
const MAX_NAME = 80;

/** É2: admin screen for the three catalogs (segments, surgery types, scar types). */
@Component({
  selector: 'app-catalogs-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './catalogs-page.html',
  styleUrl: './catalogs-page.scss',
})
export class CatalogsPage {
  private readonly catalogsService = inject(CatalogsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly catalogs = CATALOGS;
  protected readonly maxName = MAX_NAME;
  protected readonly current = signal<CatalogDefinition>(CATALOGS[0]);
  protected readonly items = signal<CatalogItem[] | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly newName = new FormControl('', { nonNullable: true });
  protected readonly addError = signal<string | null>(null);
  protected readonly adding = signal(false);

  protected readonly editingId = signal<number | null>(null);
  protected readonly editName = new FormControl('', { nonNullable: true });
  protected readonly editError = signal<string | null>(null);
  protected readonly pendingIds = signal<ReadonlySet<number>>(new Set());

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe((params) => {
      const catalog = CATALOGS.find((definition) => definition.key === params.get('tipo')) ?? CATALOGS[0];
      this.open(catalog);
    });
    this.newName.valueChanges.subscribe(() => this.addError.set(null));
    this.editName.valueChanges.subscribe(() => this.editError.set(null));
  }

  protected select(catalog: CatalogDefinition): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { tipo: catalog.key } });
  }

  protected add(): void {
    const name = this.newName.value.trim();
    if (this.adding()) {
      return;
    }
    if (name === '') {
      this.addError.set(BLANK_MESSAGE);
      return;
    }
    const slug = this.current().slug;
    this.adding.set(true);
    this.catalogsService.create(slug, name).subscribe({
      next: (created) => {
        this.adding.set(false);
        this.newName.setValue('');
        this.items.update((list) => sortByName([...(list ?? []), created]));
      },
      error: (err: unknown) => {
        this.adding.set(false);
        this.addError.set(this.saveErrorMessage(err));
      },
    });
  }

  protected startEdit(item: CatalogItem): void {
    this.editingId.set(item.id);
    this.editName.setValue(item.name);
    this.editError.set(null);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.editError.set(null);
  }

  protected saveEdit(item: CatalogItem): void {
    const name = this.editName.value.trim();
    if (this.isPending(item)) {
      return;
    }
    if (name === '') {
      this.editError.set(BLANK_MESSAGE);
      return;
    }
    this.setPending(item.id, true);
    this.catalogsService.rename(this.current().slug, item.id, name).subscribe({
      next: (renamed) => {
        this.setPending(item.id, false);
        this.editingId.set(null);
        this.replace(renamed, true);
      },
      error: (err: unknown) => {
        this.setPending(item.id, false);
        this.editError.set(this.saveErrorMessage(err));
      },
    });
  }

  protected toggle(item: CatalogItem): void {
    if (this.isPending(item)) {
      return;
    }
    this.error.set(null);
    this.setPending(item.id, true);
    this.catalogsService.setActive(this.current().slug, item.id, !item.active).subscribe({
      next: (updated) => {
        this.setPending(item.id, false);
        this.replace(updated, false);
      },
      error: (err: unknown) => {
        this.setPending(item.id, false);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  protected isPending(item: CatalogItem): boolean {
    return this.pendingIds().has(item.id);
  }

  private open(catalog: CatalogDefinition): void {
    this.current.set(catalog);
    this.items.set(null);
    this.error.set(null);
    this.addError.set(null);
    this.editingId.set(null);
    this.newName.setValue('');
    this.catalogsService.list(catalog.slug).subscribe({
      next: (items) => {
        if (this.current() === catalog) {
          this.items.set(items);
        }
      },
      error: () => this.error.set(LOAD_ERROR),
    });
  }

  private replace(updated: CatalogItem, resort: boolean): void {
    this.items.update((list) => {
      const next = (list ?? []).map((item) => (item.id === updated.id ? updated : item));
      return resort ? sortByName(next) : next;
    });
  }

  private saveErrorMessage(err: unknown): string {
    return apiErrorCode(err) === CATALOG_ERROR_CODES.duplicate ? DUPLICATE_MESSAGE : (apiErrorMessage(err) ?? SAVE_ERROR);
  }

  private setPending(id: number, pending: boolean): void {
    this.pendingIds.update((ids) => {
      const next = new Set(ids);
      if (pending) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  }
}

function sortByName(items: CatalogItem[]): CatalogItem[] {
  return [...items].sort((a, b) => a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }));
}
