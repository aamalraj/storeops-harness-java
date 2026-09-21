package com.storeops.alerts.domain;

import java.time.Instant;

/**
 * An operational alert addressed to a staff member.
 *
 * @param id alert identifier
 * @param storeId store the alert concerns
 * @param recipientId staff member the alert is for
 * @param severity how loudly to surface it
 * @param subject short headline
 * @param sourceEventType name of the domain event that produced the alert
 * @param sourceAggregateId record the originating event was about
 * @param acknowledged whether the recipient has dismissed it
 * @param createdAt creation timestamp
 */
public record Alert(
    String id,
    String storeId,
    String recipientId,
    AlertSeverity severity,
    String subject,
    String sourceEventType,
    String sourceAggregateId,
    boolean acknowledged,
    Instant createdAt) {
}
