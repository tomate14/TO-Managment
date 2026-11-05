import { AGENDA_HOURS, Appointment } from './agenda.models';

export interface PositionedAppointment {
  appointment: Appointment;
  top: number;
  height: number;
  column: number;
  columns: number;
}

const MINUTES_PER_HOUR = 60;
const TIME_LENGTH = 5; // times may come as "HH:mm:ss"; only "HH:mm" matters

function minutesOf(time: string): number {
  const [hours, minutes] = time.slice(0, TIME_LENGTH).split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

/** Offset in px of a time from the first agenda hour, or null outside 08:00–20:00. */
export function nowOffset(time: string, pxPerHour: number): number | null {
  const fromStart = minutesOf(time) - AGENDA_HOURS.first * MINUTES_PER_HOUR;
  if (fromStart < 0 || fromStart > (AGENDA_HOURS.last - AGENDA_HOURS.first) * MINUTES_PER_HOUR) {
    return null;
  }
  return (fromStart / MINUTES_PER_HOUR) * pxPerHour;
}

/** Positions a day's appointments; only transitively overlapping ones share columns. */
export function layoutDay(appointments: Appointment[], pxPerHour: number): PositionedAppointment[] {
  const sorted = [...appointments].sort((a, b) => minutesOf(a.startTime) - minutesOf(b.startTime));
  const result: PositionedAppointment[] = [];
  let cluster: PositionedAppointment[] = [];
  let columnEnds: number[] = [];
  let clusterEnd = 0;

  const closeCluster = () => {
    for (const positioned of cluster) {
      positioned.columns = columnEnds.length;
    }
    result.push(...cluster);
    cluster = [];
    columnEnds = [];
  };

  for (const appointment of sorted) {
    const start = minutesOf(appointment.startTime);
    const end = start + appointment.durationMinutes;
    if (cluster.length > 0 && start >= clusterEnd) {
      closeCluster();
    }
    let column = columnEnds.findIndex((columnEnd) => columnEnd <= start);
    if (column === -1) {
      column = columnEnds.length;
      columnEnds.push(end);
    } else {
      columnEnds[column] = end;
    }
    clusterEnd = cluster.length === 0 ? end : Math.max(clusterEnd, end);
    cluster.push({
      appointment,
      top: ((start - AGENDA_HOURS.first * MINUTES_PER_HOUR) / MINUTES_PER_HOUR) * pxPerHour,
      height: (appointment.durationMinutes / MINUTES_PER_HOUR) * pxPerHour,
      column,
      columns: 1,
    });
  }
  closeCluster();
  return result;
}
