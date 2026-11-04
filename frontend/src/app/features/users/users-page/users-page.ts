import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/api/api-error';
import { ROLE_SHORT_LABELS, userInitials } from '../../../layout/user-initials';
import { ManagedUser } from '../users.models';
import { UsersService } from '../users.service';

const LOAD_ERROR = 'No pudimos cargar los usuarios. Intentá de nuevo en unos minutos.';
const TOGGLE_ERROR = 'No pudimos cambiar el estado del usuario. Intentá de nuevo.';

/** H1.4 users list: role and status badges, edit link and an activate/deactivate switch. */
@Component({
  selector: 'app-users-page',
  imports: [RouterLink],
  templateUrl: './users-page.html',
  styleUrl: './users-page.scss',
})
export class UsersPage implements OnInit {
  private readonly usersService = inject(UsersService);
  private readonly myId = inject(AuthService).currentUser()?.id ?? null;

  protected readonly users = signal<ManagedUser[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly pendingIds = signal<ReadonlySet<number>>(new Set());
  protected readonly roleLabels = ROLE_SHORT_LABELS;
  protected readonly initials = userInitials;

  ngOnInit(): void {
    this.usersService.list().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.error.set(LOAD_ERROR),
    });
  }

  protected isMe(user: ManagedUser): boolean {
    return user.id === this.myId;
  }

  protected isPending(user: ManagedUser): boolean {
    return this.pendingIds().has(user.id);
  }

  protected toggle(user: ManagedUser): void {
    if (this.isMe(user) || this.isPending(user)) {
      return;
    }
    this.error.set(null);
    this.setPending(user.id, true);
    this.usersService.setActive(user.id, !user.active).subscribe({
      next: (updated) => {
        this.setPending(user.id, false);
        this.users.update((list) => list?.map((item) => (item.id === updated.id ? updated : item)) ?? null);
      },
      error: (err: unknown) => {
        this.setPending(user.id, false);
        this.error.set(apiErrorMessage(err) ?? TOGGLE_ERROR);
      },
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
