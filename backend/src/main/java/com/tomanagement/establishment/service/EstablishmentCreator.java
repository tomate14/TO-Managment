package com.tomanagement.establishment.service;

import com.tomanagement.establishment.entity.Establishment;

/**
 * Creates an establishment, its ADMIN user and its default catalogs. Shared by the superadmin flow and the
 * independent therapist self-registration; it runs in the caller's transaction.
 */
public interface EstablishmentCreator {

    /** Does not check name uniqueness: that decision belongs to the caller. */
    Establishment create(NewEstablishment request);
}
