import { TestBed } from '@angular/core/testing';
import { appointmentOf } from '../agenda-test-data';
import { Appointment } from '../agenda.models';
import { WeekCalendar } from './week-calendar';

describe('WeekCalendar', () => {
  const days = ['2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05', '2026-11-06', '2026-11-07', '2026-11-08'];

  function render(appointments: Appointment[], now = '11:40') {
    const fixture = TestBed.createComponent(WeekCalendar);
    fixture.componentRef.setInput('days', days);
    fixture.componentRef.setInput('appointments', appointments);
    fixture.componentRef.setInput('today', '2026-11-05');
    fixture.componentRef.setInput('now', now);
    fixture.detectChanges();
    const selected: Appointment[] = [];
    fixture.componentInstance.select.subscribe((appointment) => selected.push(appointment));
    return { el: fixture.nativeElement as HTMLElement, selected };
  }

  const nineAm = appointmentOf({
    id: 1,
    therapist: { id: 2, fullName: 'Carla Ríos', color: 2 },
  });

  it('places a 45-minute appointment at 09:00 with its color', () => {
    const { el } = render([nineAm]);
    const block = el.querySelector('.agenda-block') as HTMLElement;
    expect(block.style.top).toBe('52px');
    expect(block.style.height).toBe('39px');
    expect(block.classList).toContain('therapist-2');
    expect(block.textContent).toContain('Lucía Fernández');
    expect(block.textContent).toContain('09:00–09:45');
  });

  it('emits the appointment when a block is clicked', () => {
    const { el, selected } = render([nineAm]);
    (el.querySelector('.agenda-block') as HTMLElement).click();
    expect(selected).toEqual([nineAm]);
  });

  it('splits overlapping appointments into columns', () => {
    const other = appointmentOf({ id: 2, startTime: '09:00:00', endTime: '10:00:00', durationMinutes: 60 });
    const { el } = render([nineAm, other]);
    const blocks = Array.from(el.querySelectorAll<HTMLElement>('.agenda-block'));
    expect(blocks.map((block) => block.style.width)).toEqual(['50%', '50%']);
  });

  it('draws the now line only on today inside the agenda hours', () => {
    expect(render([]).el.querySelectorAll('.week__now')).toHaveLength(1);
    expect(render([], '21:00').el.querySelector('.week__now')).toBeNull();
  });
});
