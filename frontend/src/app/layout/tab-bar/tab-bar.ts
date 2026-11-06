import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SUPERADMIN_TAB_ITEMS, TAB_ITEMS } from '../nav-items';

@Component({
  selector: 'app-tab-bar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './tab-bar.html',
  styleUrl: './tab-bar.scss',
})
export class TabBar {
  private readonly auth = inject(AuthService);
  protected readonly items = computed(() => {
    const user = this.auth.currentUser();
    return user?.role === 'SUPERADMIN' && !user.establishment ? SUPERADMIN_TAB_ITEMS : TAB_ITEMS;
  });
}
