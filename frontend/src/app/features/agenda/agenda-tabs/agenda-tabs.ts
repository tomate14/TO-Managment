import { Component, ElementRef, inject, input, output } from '@angular/core';

export type AgendaView = 'calendario' | 'turnos';

const VIEWS: readonly AgendaView[] = ['calendario', 'turnos'];

/** Accessible two-tab switch (Calendario / Turnos asignados) with arrow-key navigation. */
@Component({
  selector: 'app-agenda-tabs',
  templateUrl: './agenda-tabs.html',
  styleUrl: './agenda-tabs.scss',
})
export class AgendaTabs {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly active = input.required<AgendaView>();
  readonly change = output<AgendaView>();

  protected select(view: AgendaView): void {
    if (view !== this.active()) {
      this.change.emit(view);
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    const step = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
    if (step === 0) {
      return;
    }
    event.preventDefault();
    const next = VIEWS[(VIEWS.indexOf(this.active()) + step + VIEWS.length) % VIEWS.length];
    this.change.emit(next);
    this.host.nativeElement.querySelectorAll<HTMLElement>('[role="tab"]')[VIEWS.indexOf(next)]?.focus();
  }
}
