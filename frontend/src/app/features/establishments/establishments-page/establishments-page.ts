import { Component, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { injectIsDesktop } from '../../../layout/desktop-media';
import { userInitials } from '../../../layout/user-initials';
import { EstablishmentForm } from '../establishment-form/establishment-form';
import { ESTABLISHMENT_ERROR_CODES, EstablishmentSummary } from '../establishments.models';
import { EstablishmentsService } from '../establishments.service';

const NEW_URL = '/establecimientos/nuevo';
const HOME_URL = '/pacientes';
const MAX_NAME = 120;
const BLANK_MESSAGE = 'Ingresá un nombre.';
const TAKEN_MESSAGE = 'Ya hay un establecimiento con ese nombre.';
const LOAD_ERROR = 'No pudimos cargar los establecimientos. Intentá de nuevo en unos minutos.';
const SAVE_ERROR = 'No pudimos guardar el cambio. Intentá de nuevo.';
const ENTER_ERROR = 'No pudimos entrar al establecimiento. Intentá de nuevo.';

/** H8.2 superadmin list of establishments: cards on mobile, table on desktop (with the H8.1 side panel). */
@Component({
  selector: 'app-establishments-page',
  imports: [ReactiveFormsModule, NgTemplateOutlet, EstablishmentForm],
  templateUrl: './establishments-page.html',
  styleUrl: './establishments-page.scss',
})
export class EstablishmentsPage {
  private readonly establishmentsService = inject(EstablishmentsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isDesktop = injectIsDesktop();
  protected readonly initials = userInitials;
  protected readonly maxName = MAX_NAME;

  protected readonly establishments = signal<EstablishmentSummary[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly panelOpen = signal(false);

  protected readonly editingId = signal<number | null>(null);
  protected readonly editName = new FormControl('', { nonNullable: true });
  protected readonly editError = signal<string | null>(null);
  protected readonly pendingIds = signal<ReadonlySet<number>>(new Set());

  constructor() {
    this.editName.valueChanges.subscribe(() => this.editError.set(null));
    this.establishmentsService.list().subscribe({
      next: (list) => this.establishments.set(list),
      error: () => this.error.set(LOAD_ERROR),
    });
  }

  protected openNew(): void {
    if (this.isDesktop()) {
      this.panelOpen.set(true);
    } else {
      void this.router.navigateByUrl(NEW_URL);
    }
  }

  protected closePanel(): void {
    this.panelOpen.set(false);
  }

  protected onCreated(created: EstablishmentSummary): void {
    this.panelOpen.set(false);
    this.establishments.update((list) => sortByName([...(list ?? []), created]));
  }

  protected admin(item: EstablishmentSummary): { fullName: string; email: string } | null {
    return item.admins[0] ?? null;
  }

  protected usersLabel(count: number): string {
    return count === 1 ? '1 usuario' : `${count} usuarios`;
  }

  protected startEdit(item: EstablishmentSummary): void {
    this.editingId.set(item.id);
    this.editName.setValue(item.name);
    this.editError.set(null);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.editError.set(null);
  }

  protected saveEdit(item: EstablishmentSummary): void {
    const name = this.editName.value.trim();
    if (this.isPending(item)) {
      return;
    }
    if (name === '') {
      this.editError.set(BLANK_MESSAGE);
      return;
    }
    this.setPending(item.id, true);
    this.establishmentsService.rename(item.id, name).subscribe({
      next: (renamed) => {
        this.setPending(item.id, false);
        this.editingId.set(null);
        this.replace(renamed, true);
      },
      error: (err: unknown) => {
        this.setPending(item.id, false);
        this.editError.set(
          apiErrorCode(err) === ESTABLISHMENT_ERROR_CODES.nameTaken ? TAKEN_MESSAGE : (apiErrorMessage(err) ?? SAVE_ERROR),
        );
      },
    });
  }

  protected toggle(item: EstablishmentSummary): void {
    if (this.isPending(item)) {
      return;
    }
    this.error.set(null);
    this.setPending(item.id, true);
    this.establishmentsService.setActive(item.id, !item.active).subscribe({
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

  protected enter(item: EstablishmentSummary): void {
    if (!item.active || this.isPending(item)) {
      return;
    }
    this.error.set(null);
    this.setPending(item.id, true);
    this.auth.enterEstablishment(item.id).subscribe({
      next: () => {
        this.setPending(item.id, false);
        void this.router.navigateByUrl(HOME_URL);
      },
      error: (err: unknown) => {
        this.setPending(item.id, false);
        this.error.set(apiErrorMessage(err) ?? ENTER_ERROR);
      },
    });
  }

  protected isPending(item: EstablishmentSummary): boolean {
    return this.pendingIds().has(item.id);
  }

  private replace(updated: EstablishmentSummary, resort: boolean): void {
    this.establishments.update((list) => {
      const next = (list ?? []).map((item) => (item.id === updated.id ? updated : item));
      return resort ? sortByName(next) : next;
    });
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

function sortByName(items: EstablishmentSummary[]): EstablishmentSummary[] {
  return [...items].sort((a, b) => a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }));
}
