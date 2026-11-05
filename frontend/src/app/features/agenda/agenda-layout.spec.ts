import { layoutDay, nowOffset } from './agenda-layout';
import { Appointment } from './agenda.models';

function appt(id: number, start: string, end: string): Appointment {
  const minutes = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3, 5));
  return {
    id,
    therapist: { id: 1, fullName: 'Ana Pérez', color: 1 },
    patient: { id: 1, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' },
    diagnosis: null,
    date: '2026-11-05',
    startTime: `${start}:00`,
    endTime: `${end}:00`,
    durationMinutes: minutes(end) - minutes(start),
    notes: null,
  };
}

describe('layoutDay', () => {
  it('splits only overlapping appointments into columns', () => {
    const layout = layoutDay([appt(1, '08:30', '09:15'), appt(2, '09:00', '09:45'), appt(3, '10:30', '11:30')], 64);
    expect(layout.map((p) => [p.appointment.id, p.column, p.columns])).toEqual([[1, 0, 2], [2, 1, 2], [3, 0, 1]]);
    expect(layout[0].top).toBe(32);
    expect(layout[2].height).toBe(64);
  });

  it('reuses a freed column inside the same cluster', () => {
    const layout = layoutDay([appt(1, '09:00', '10:00'), appt(2, '09:30', '10:30'), appt(3, '10:00', '10:30')], 52);
    expect(layout.find((p) => p.appointment.id === 3)!.column).toBe(0);
  });
});

describe('nowOffset', () => {
  it('places the now line only inside agenda hours', () => {
    expect(nowOffset('11:40', 64)).toBeCloseTo(234.67, 1);
    expect(nowOffset('21:00', 64)).toBeNull();
    expect(nowOffset('07:59', 64)).toBeNull();
  });
});
