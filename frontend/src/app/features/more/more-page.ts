import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ROLE_LABELS, userInitials } from '../../layout/user-initials';

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
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly initials = userInitials;

  protected logout(): void {
    this.auth.logout();
  }
}
