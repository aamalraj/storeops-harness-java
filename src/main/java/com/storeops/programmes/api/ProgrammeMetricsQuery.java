package com.storeops.programmes.api;

/**
 * Read-only programme counts published for other modules (today: reports).
 *
 * <p>Keeps reports out of the programmes service and programmes out of reports entirely.
 */
public interface ProgrammeMetricsQuery {

  /** Total programmes at a store. */
  long countAll(String storeId);

  /** Programmes at a store that are currently ACTIVE. */
  long countActive(String storeId);
}
