package com.tomanagement.establishment.service;

/** Answers whether an establishment's trial has ended; used on every authenticated request. */
public interface TrialStatusChecker {

    boolean isExpired(Long establishmentId);
}
