package com.tomanagement.billing.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.tomanagement.billing.repository.BillingFollowUpRepository;
import com.tomanagement.followup.service.BilledFollowUpLookup;

import org.springframework.stereotype.Service;

/**
 * Adapter: exposes only historical billing state through the follow-up port. It reads only
 * repositories, so it creates no bean cycle with the services that use it.
 */
@Service
public class BilledFollowUpAdapter implements BilledFollowUpLookup {

    private final BillingFollowUpRepository linkRepository;

    public BilledFollowUpAdapter(BillingFollowUpRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Override
    public Set<Long> billedAmong(Collection<Long> followUpIds) {
        if (followUpIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(linkRepository.findBilledAmong(followUpIds));
    }
}
