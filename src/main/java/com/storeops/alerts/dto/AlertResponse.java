package com.storeops.alerts.dto;

import com.storeops.alerts.domain.Alert;
import com.storeops.alerts.domain.AlertSeverity;
import java.time.Instant;

/** Wire representation of an alert. */
public record AlertResponse(
    String id,
    String storeId,
    String recipientId,
    AlertSeverity severity,
    String subject,
    String sourceEventType,
    String sourceAggregateId,
    boolean acknowledged,
    Instant createdAt) {

  public static AlertResponse from(Alert alert) {
    return new AlertResponse(
        alert.id(),
        alert.storeId(),
        alert.recipientId(),
        alert.severity(),
        alert.subject(),
        alert.sourceEventType(),
        alert.sourceAggregateId(),
        alert.acknowledged(),
        alert.createdAt());
  }
}
