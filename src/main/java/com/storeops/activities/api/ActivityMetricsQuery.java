package com.storeops.activities.api;

import java.util.Map;

/**
 * Read-only activity counts published for other modules (today: reports).
 *
 * <p>Reports depends on this port rather than on {@code ActivityService}, and activities never
 * depends on reports — that is what keeps the two modules acyclic. Statuses are returned as strings
 * so consumers need not import the activities domain enums.
 */
public interface ActivityMetricsQuery {

  /** Number of activities in each status for a store, keyed by status name. */
  Map<String, Long> countByStatus(String storeId);

  /** Number of activities in a store that are neither DONE nor CANCELLED. */
  long countOpen(String storeId);
}
