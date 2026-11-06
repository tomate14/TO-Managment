import { Component, OnInit, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { EstablishmentBanner } from '../establishment-banner/establishment-banner';
import { SideNav } from '../side-nav/side-nav';
import { TabBar } from '../tab-bar/tab-bar';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, SideNav, TabBar, EstablishmentBanner],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  private readonly auth = inject(AuthService);
  protected readonly superadminInside = computed(() => {
    const user = this.auth.currentUser();
    return user?.role === 'SUPERADMIN' && !!user.establishment;
  });

  ngOnInit(): void {
    this.auth.refreshCurrentUser();
  }
}
