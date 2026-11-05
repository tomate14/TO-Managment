import { TestBed } from '@angular/core/testing';
import { AgendaTabs } from './agenda-tabs';

describe('AgendaTabs', () => {
  function render(active: 'calendario' | 'turnos') {
    const fixture = TestBed.createComponent(AgendaTabs);
    fixture.componentRef.setInput('active', active);
    fixture.detectChanges();
    const changes: string[] = [];
    fixture.componentInstance.change.subscribe((view) => changes.push(view));
    const tabs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('[role="tab"]'));
    return { fixture, tabs, changes };
  }

  it('marks the active tab and gives the others tabindex -1', () => {
    const { tabs } = render('turnos');
    expect(tabs.map((t) => t.getAttribute('aria-selected'))).toEqual(['false', 'true']);
    expect(tabs.map((t) => t.getAttribute('tabindex'))).toEqual(['-1', '0']);
  });

  it('moves between tabs with the arrow keys', () => {
    const { tabs, changes } = render('calendario');
    tabs[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    expect(changes).toEqual(['turnos']);
  });

  it('wraps with ArrowLeft from the first tab', () => {
    const { tabs, changes } = render('calendario');
    tabs[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    expect(changes).toEqual(['turnos']);
  });

  it('emits when a tab is clicked', () => {
    const { tabs, changes } = render('calendario');
    tabs[1].click();
    expect(changes).toEqual(['turnos']);
  });
});
