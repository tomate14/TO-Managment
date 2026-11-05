import { TestBed } from '@angular/core/testing';
import { appointmentOf } from '../agenda-test-data';
import { WeekStrip } from './week-strip';

describe('WeekStrip', () => {
  const days = ['2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05', '2026-11-06', '2026-11-07', '2026-11-08'];

  function render() {
    const fixture = TestBed.createComponent(WeekStrip);
    fixture.componentRef.setInput('days', days);
    fixture.componentRef.setInput('selected', '2026-11-05');
    fixture.componentRef.setInput('today', '2026-11-05');
    fixture.componentRef.setInput('appointments', [
      appointmentOf({ id: 1 }),
      appointmentOf({ id: 2, startTime: '11:00:00', endTime: '11:45:00' }),
      appointmentOf({ id: 3, therapist: { id: 2, fullName: 'Carla Ríos', color: 2 } }),
      appointmentOf({ id: 4, date: '2026-11-06' }),
    ]);
    fixture.detectChanges();
    const picks: string[] = [];
    const shifts: number[] = [];
    fixture.componentInstance.pick.subscribe((date) => picks.push(date));
    fixture.componentInstance.shift.subscribe((direction) => shifts.push(direction));
    return { el: fixture.nativeElement as HTMLElement, picks, shifts };
  }

  it('shows one dot per therapist with appointments that day', () => {
    const { el } = render();
    const buttons = Array.from(el.querySelectorAll<HTMLElement>('.strip__day'));
    expect(buttons[3].querySelectorAll('.strip__dot')).toHaveLength(2);
    expect(buttons[4].querySelectorAll('.strip__dot')).toHaveLength(1);
    expect(buttons[0].querySelectorAll('.strip__dot')).toHaveLength(0);
  });

  it('emits the picked day and the week shift', () => {
    const { el, picks, shifts } = render();
    el.querySelectorAll<HTMLElement>('.strip__day')[1].click();
    el.querySelector<HTMLElement>('[aria-label="Semana siguiente"]')!.click();
    expect(picks).toEqual(['2026-11-03']);
    expect(shifts).toEqual([1]);
  });
});
