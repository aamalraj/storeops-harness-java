package com.storeops.reports.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Point-in-time operational metrics for one store.
 *
 * @param storeId store the metrics describe
 * @param activitiesByStatus activity counts keyed by status name
 * @param openActivities activities not yet DONE or CANCELLED
 * @param totalProgrammes programmes at the store
 * @param activeProgrammes programmes currently ACTIVE
 * @param generatedAt when the figures were computed
 */
public record StoreMetrics(
    String storeId,
    Map<String, Long> activitiesByStatus,
    long openActivities,
    long totalProgrammes,
    long activeProgrammes,
    Instant generatedAt) {

  public StoreMetrics {
    activitiesByStatus = Map.copyOf(activitiesByStatus);
  }
}
