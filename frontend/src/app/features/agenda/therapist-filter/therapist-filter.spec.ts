import { TestBed } from '@angular/core/testing';
import { AgendaTherapist } from '../agenda.models';
import { TherapistFilter } from './therapist-filter';

describe('TherapistFilter', () => {
  const therapists: AgendaTherapist[] = [
    { id: 1, fullName: 'Mariana Gómez', color: 1, active: true },
    { id: 2, fullName: 'Carla Ríos', color: 2, active: true },
  ];

  it('counts appointments per therapist and toggles', () => {
    const fixture = TestBed.createComponent(TherapistFilter);
    fixture.componentRef.setInput('therapists', therapists);
    fixture.componentRef.setInput('selected', new Set([1]));
    fixture.componentRef.setInput('counts', new Map([[1, 6]]));
    fixture.detectChanges();
    const toggled: number[] = [];
    fixture.componentInstance.toggle.subscribe((id) => toggled.push(id));
    const el = fixture.nativeElement as HTMLElement;

    const counts = Array.from(el.querySelectorAll('.therapist-filter__count')).map((c) => c.textContent?.trim());
    expect(counts).toEqual(['6', '0']);
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('input'));
    expect(boxes.map((box) => box.checked)).toEqual([true, false]);

    boxes[1].click();
    expect(toggled).toEqual([2]);
  });
});
