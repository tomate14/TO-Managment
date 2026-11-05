import { TestBed } from '@angular/core/testing';
import { appointmentOf } from '../agenda-test-data';
import { Appointment } from '../agenda.models';
import { DayTimeline } from './day-timeline';

describe('DayTimeline', () => {
  function render(appointments: Appointment[], date = '2026-11-05') {
    const fixture = TestBed.createComponent(DayTimeline);
    fixture.componentRef.setInput('date', date);
    fixture.componentRef.setInput('appointments', appointments);
    fixture.componentRef.setInput('today', '2026-11-05');
    fixture.componentRef.setInput('now', '11:40');
    fixture.detectChanges();
    const selected: Appointment[] = [];
    fixture.componentInstance.select.subscribe((appointment) => selected.push(appointment));
    return { el: fixture.nativeElement as HTMLElement, selected };
  }

  it('shows the day heading, count and a block with the therapist first name', () => {
    const { el } = render([
      appointmentOf({ id: 1 }),
      appointmentOf({ id: 2, date: '2026-11-06' }),
    ]);
    expect(el.textContent).toContain('Hoy · jueves 5 de noviembre');
    expect(el.textContent).toContain('1 turno');
    const block = el.querySelector('.agenda-block') as HTMLElement;
    expect(block.style.top).toBe('64px');
    expect(block.textContent).toContain('09:00–09:45 · Mariana');
    expect(el.querySelectorAll('.agenda-block')).toHaveLength(1);
  });

  it('emits the appointment when a block is clicked', () => {
    const appointment = appointmentOf({ id: 1 });
    const { el, selected } = render([appointment]);
    (el.querySelector('.agenda-block') as HTMLElement).click();
    expect(selected).toEqual([appointment]);
  });

  it('shows the now line only when the day is today', () => {
    expect(render([]).el.querySelector('.day__now')).toBeTruthy();
    expect(render([], '2026-11-06').el.querySelector('.day__now')).toBeNull();
  });
});
