import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { NAV_ITEMS } from '../nav-items';
import { ROLE_LABELS, userInitials } from '../user-initials';

@Component({
  selector: 'app-side-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './side-nav.html',
  styleUrl: './side-nav.scss',
})
export class SideNav {
  protected readonly items = NAV_ITEMS;
  private readonly auth = inject(AuthService);
  protected readonly user = this.auth.currentUser;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly initials = userInitials;

  protected logout(): void {
    this.auth.logout();
  }
}
