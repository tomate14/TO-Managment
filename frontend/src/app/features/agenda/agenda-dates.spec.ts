import { addDays, dayHeading, groupByDay, monthGrid, weekDays, weekStart, weekTitle } from './agenda-dates';
import { Appointment } from './agenda.models';

function appt(id: number, date: string, startTime: string): Appointment {
  return {
    id,
    therapist: { id: 1, fullName: 'Ana Pérez', color: 1 },
    patient: { id: 1, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' },
    diagnosis: null,
    date,
    startTime,
    endTime: startTime,
    durationMinutes: 30,
    notes: null,
  };
}

describe('agenda dates', () => {
  it('builds week titles and headings', () => {
    expect(weekDays('2026-11-05')[0]).toBe('2026-11-02');
    expect(weekTitle('2026-11-05')).toBe('2 – 8 de noviembre de 2026');
    expect(weekTitle('2026-12-02')).toBe('30 de noviembre – 6 de diciembre de 2026');
    expect(dayHeading('2026-11-05', '2026-11-05')).toBe('Hoy · jueves 5 de noviembre');
    expect(dayHeading('2026-11-06', '2026-11-05')).toBe('Mañana · viernes 6 de noviembre');
    expect(dayHeading('2026-11-07', '2026-11-05')).toBe('Sábado 7 de noviembre');
    expect(monthGrid('2026-11-05')[0]).toBe('2026-10-26');
    expect(monthGrid('2026-11-05')).toHaveLength(42);
  });

  it('handles week boundaries and year changes', () => {
    expect(weekStart('2026-11-08')).toBe('2026-11-02');
    expect(weekTitle('2026-12-30')).toBe('28 de diciembre de 2026 – 3 de enero de 2027');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('groups appointments by day in order', () => {
    const groups = groupByDay([appt(1, '2026-11-06', '10:00:00'), appt(2, '2026-11-05', '11:00:00'), appt(3, '2026-11-05', '09:00:00')]);
    expect(groups.map((g) => [g.date, g.appointments.map((a) => a.id)])).toEqual([
      ['2026-11-05', [3, 2]],
      ['2026-11-06', [1]],
    ]);
  });
});
