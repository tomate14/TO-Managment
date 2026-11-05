import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, filter, merge, tap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { injectIsDesktop } from '../../../layout/desktop-media';
import { userInitials } from '../../../layout/user-initials';
import { dominanceLabel, formatDni, listName, patientInitials } from '../patient-format';
import { Patient } from '../patients.models';
import { PatientsService } from '../patients.service';

const SEARCH_DEBOUNCE_MS = 1000;
const LOAD_ERROR = 'No pudimos cargar los pacientes. Intentá de nuevo en unos minutos.';

/**
 * H4.2 patient search. Mobile appends pages with "Cargar más"; desktop pages with "Anterior" / "Siguiente".
 */
@Component({
  selector: 'app-patients-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './patients-page.html',
  styleUrl: './patients-page.scss',
})
export class PatientsPage {
  private readonly patientsService = inject(PatientsService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly user = inject(AuthService).currentUser;
  protected readonly isDesktop = injectIsDesktop();

  protected readonly query = new FormControl('', { nonNullable: true });
  protected readonly patients = signal<Patient[] | null>(null);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Text of the last search actually sent (after the debounce). */
  protected readonly searchedText = signal('');

  protected readonly hasMore = computed(() => this.page() + 1 < this.totalPages());
  protected readonly countLabel = computed(() =>
    this.searchedText() === '' ? 'Ordenados por apellido' : `${this.totalElements()} resultados`);

  protected readonly formatDni = formatDni;
  protected readonly listName = listName;
  protected readonly initials = patientInitials;
  protected readonly dominance = dominanceLabel;
  protected readonly userInitials = userInitials;

  private requestId = 0;
  private lastSearched: string | null = '';
  /** Emits the current search text when Enter is pressed, bypassing the debounce. */
  private readonly searchNow = new Subject<string>();

  constructor() {
    // The debounced text and Enter feed one stream; the same text is never requested twice in a row.
    merge(this.query.valueChanges.pipe(debounceTime(SEARCH_DEBOUNCE_MS)), this.searchNow)
      .pipe(
        // Skip only a repeat of the text in flight or already shown; a failed load resets it.
        filter((text) => text !== this.lastSearched),
        tap((text) => (this.lastSearched = text)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((text) => {
        this.searchedText.set(text.trim());
        this.load(0, false);
      });
    // Reload from the first page when the layout (and so the paging style) changes.
    effect(() => {
      this.isDesktop();
      untracked(() => this.load(0, false));
    });
  }

  protected searchOnEnter(event: Event): void {
    event.preventDefault();
    this.searchNow.next(this.query.value);
  }

  protected loadMore(): void {
    if (this.hasMore() && !this.loading()) {
      this.load(this.page() + 1, true);
    }
  }

  protected goTo(page: number): void {
    if (page >= 0 && page < this.totalPages() && !this.loading()) {
      this.load(page, false);
    }
  }

  private load(page: number, append: boolean): void {
    const id = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    this.patientsService.search(this.searchedText(), page).subscribe({
      next: (result) => {
        if (id !== this.requestId) {
          return;
        }
        this.loading.set(false);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(result.totalElements);
        this.patients.update((current) => (append ? [...(current ?? []), ...result.content] : result.content));
      },
      error: () => {
        if (id === this.requestId) {
          this.lastSearched = null;
          this.loading.set(false);
          this.error.set(LOAD_ERROR);
        }
      },
    });
  }
}
