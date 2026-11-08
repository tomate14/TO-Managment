import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { isAdminInEstablishment, isIndividualPractice } from '../../core/auth/auth.models';
import { EstablishmentsService } from '../../features/establishments/establishments.service';
import { NAV_ITEMS, SUPERADMIN_NAV_ITEMS } from '../nav-items';
import { roleLabelFor, userInitials } from '../user-initials';

@Component({
  selector: 'app-side-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './side-nav.html',
  styleUrl: './side-nav.scss',
})
export class SideNav {
  private readonly auth = inject(AuthService);
  private readonly establishments = inject(EstablishmentsService);
  protected readonly pendingCount = this.establishments.pendingCount;
  protected readonly pendingPath = SUPERADMIN_NAV_ITEMS[0].path;
  protected readonly user = this.auth.currentUser;
  protected readonly establishmentName = computed(() => this.user()?.establishment?.name ?? null);
  protected readonly items = computed(() =>
    this.user()?.role === 'SUPERADMIN' && this.establishmentName() === null ? SUPERADMIN_NAV_ITEMS : NAV_ITEMS,
  );
  protected readonly showAdmin = computed(() => isAdminInEstablishment(this.user()));
  protected readonly individual = computed(() => isIndividualPractice(this.user()));
  protected readonly roleLabel = roleLabelFor;
  protected readonly initials = userInitials;

  protected logout(): void {
    this.auth.logout();
  }
}
