import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Section without content yet (its story is pending); only an accessible heading. */
@Component({
  selector: 'app-empty-section',
  template: '<h1 class="visually-hidden">{{ heading }}</h1>',
})
export class EmptySection {
  protected readonly heading: string = inject(ActivatedRoute).snapshot.data['heading'] ?? '';
}
