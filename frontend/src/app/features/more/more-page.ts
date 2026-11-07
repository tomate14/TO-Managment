import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { isAdminInEstablishment, isIndividualPractice } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { roleLabelFor, userInitials } from '../../layout/user-initials';

/** Mobile "Más" menu: account card, admin section and account actions (more rows arrive with H1.5 and É2). */
@Component({
  selector: 'app-more-page',
  imports: [RouterLink],
  templateUrl: './more-page.html',
  styleUrl: './more-page.scss',
})
export class MorePage {
  private readonly auth = inject(AuthService);
  protected readonly user = this.auth.currentUser;
  protected readonly showAdmin = computed(() => isAdminInEstablishment(this.user()));
  protected readonly individual = computed(() => isIndividualPractice(this.user()));
  protected readonly roleLabel = roleLabelFor;
  protected readonly initials = userInitials;

  protected logout(): void {
    this.auth.logout();
  }
}
