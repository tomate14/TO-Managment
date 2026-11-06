import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { EstablishmentForm } from '../establishment-form/establishment-form';

const LIST_URL = '/establecimientos';

/** H8.1 mobile: new establishment at `/establecimientos/nuevo` (desktop uses a side panel in the list). */
@Component({
  selector: 'app-establishment-form-page',
  imports: [EstablishmentForm, RouterLink],
  templateUrl: './establishment-form-page.html',
  styleUrl: './establishment-form-page.scss',
})
export class EstablishmentFormPage {
  private readonly router = inject(Router);

  protected backToList(): void {
    void this.router.navigateByUrl(LIST_URL);
  }
}
