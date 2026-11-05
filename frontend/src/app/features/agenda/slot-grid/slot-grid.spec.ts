import { TestBed } from '@angular/core/testing';
import { Slot } from '../agenda.models';
import { SlotGrid } from './slot-grid';

describe('SlotGrid', () => {
  const slots: Slot[] = [
    { startTime: '09:00:00', available: false, reason: 'THERAPIST_BUSY' },
    { startTime: '09:30:00', available: true, reason: null },
  ];

  function setup(selected: string | null = null) {
    const fixture = TestBed.createComponent(SlotGrid);
    fixture.componentRef.setInput('slots', slots);
    fixture.componentRef.setInput('selected', selected);
    fixture.detectChanges();
    return fixture;
  }

  it('disables unavailable slots and labels them', () => {
    const el = setup().nativeElement as HTMLElement;
    const busy = el.querySelector('button[aria-label="09:00, no disponible"]') as HTMLButtonElement;
    expect(busy.disabled).toBe(true);
    expect(busy.textContent?.trim()).toBe('09:00');
  });

  it('marks the selected slot and emits the picked time as HH:mm', () => {
    const fixture = setup('09:30');
    const picked: string[] = [];
    fixture.componentInstance.pick.subscribe((time) => picked.push(time));
    const button = (fixture.nativeElement as HTMLElement).querySelector('button[aria-label="09:30"]') as HTMLButtonElement;
    expect(button.getAttribute('aria-pressed')).toBe('true');
    button.click();
    expect(picked).toEqual(['09:30']);
  });
});
