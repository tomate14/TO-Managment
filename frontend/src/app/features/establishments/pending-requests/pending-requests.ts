import { Component, input, output } from '@angular/core';
import { userInitials } from '../../../layout/user-initials';
import { formatDate } from '../../patients/patient-format';
import { EstablishmentSummary } from '../establishments.models';

/** H9 pending independent-practice requests (amber card on mobile, amber panel on desktop). */
@Component({
  selector: 'app-pending-requests',
  templateUrl: './pending-requests.html',
  styleUrl: './pending-requests.scss',
})
export class PendingRequests {
  readonly requests = input.required<EstablishmentSummary[]>();
  readonly busyIds = input<ReadonlySet<number>>(new Set());
  readonly confirmingId = input<number | null>(null);

  readonly approve = output<EstablishmentSummary>();
  readonly askReject = output<EstablishmentSummary>();
  readonly cancelReject = output<void>();
  readonly confirmReject = output<EstablishmentSummary>();

  protected readonly initials = userInitials;

  /** The therapist herself is the practice's admin. */
  protected nameOf(item: EstablishmentSummary): string {
    return item.admins[0]?.fullName ?? item.name;
  }

  protected requestedOn(item: EstablishmentSummary): string {
    return formatDate(localIsoDate(item.createdAt));
  }
}

/** Local calendar date (yyyy-mm-dd) of an ISO instant, so late-evening requests keep the user's day. */
function localIsoDate(instant: string): string {
  const date = new Date(instant);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
