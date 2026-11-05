import { TestBed } from '@angular/core/testing';
import { MiniMonth } from './mini-month';

describe('MiniMonth', () => {
  function render() {
    const fixture = TestBed.createComponent(MiniMonth);
    fixture.componentRef.setInput('month', '2026-11-05');
    fixture.componentRef.setInput('selectedWeek', [
      '2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05', '2026-11-06', '2026-11-07', '2026-11-08',
    ]);
    fixture.componentRef.setInput('today', '2026-11-05');
    fixture.detectChanges();
    const picks: string[] = [];
    const shifts: number[] = [];
    fixture.componentInstance.pick.subscribe((date) => picks.push(date));
    fixture.componentInstance.shift.subscribe((direction) => shifts.push(direction));
    return { el: fixture.nativeElement as HTMLElement, picks, shifts };
  }

  it('renders the month title and 42 days with the visible week highlighted', () => {
    const { el } = render();
    expect(el.textContent).toContain('Noviembre 2026');
    expect(el.querySelectorAll('.mini__day')).toHaveLength(42);
    expect(el.querySelectorAll('.mini__cell--week')).toHaveLength(7);
    expect(el.querySelectorAll('.mini__day--today')).toHaveLength(1);
  });

  it('emits the picked day and the month shift', () => {
    const { el, picks, shifts } = render();
    el.querySelector<HTMLElement>('[aria-label="2026-11-10"]')!.click();
    el.querySelector<HTMLElement>('[aria-label="Mes anterior"]')!.click();
    expect(picks).toEqual(['2026-11-10']);
    expect(shifts).toEqual([-1]);
  });
});
