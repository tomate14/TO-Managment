import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

const ESTABLISHMENTS_URL = '/establecimientos';

/** Fixed notice of the superadmin inside an establishment, with the way out (H8.3). */
@Component({
  selector: 'app-establishment-banner',
  templateUrl: './establishment-banner.html',
  styleUrl: './establishment-banner.scss',
})
export class EstablishmentBanner {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly establishment = this.auth.establishment;

  protected leave(): void {
    this.auth.enterEstablishment(null).subscribe({
      next: () => void this.router.navigate([ESTABLISHMENTS_URL]),
    });
  }
}
