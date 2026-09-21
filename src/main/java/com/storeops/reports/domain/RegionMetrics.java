package com.storeops.reports.domain;

import java.time.Instant;
import java.util.List;

/**
 * Operational metrics for a region, rolled up from its stores.
 *
 * @param regionId region the metrics describe
 * @param stores per-store figures
 * @param openActivities open activities across the region
 * @param activeProgrammes active programmes across the region
 * @param generatedAt when the figures were computed
 */
public record RegionMetrics(
    String regionId,
    List<StoreMetrics> stores,
    long openActivities,
    long activeProgrammes,
    Instant generatedAt) {

  public RegionMetrics {
    stores = List.copyOf(stores);
  }
}
