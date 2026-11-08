package com.tomanagement.followup.service;

import java.util.Collection;
import java.util.Set;

/**
 * Port: which of these follow-ups are in an active billing. Implemented by the billing feature, so the follow-up
 * package does not depend on it.
 */
public interface BilledFollowUpLookup {

    Set<Long> billedAmong(Collection<Long> followUpIds);
}
