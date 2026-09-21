package com.storeops.reports.repository;

import java.util.List;

/**
 * Persistence port for the reports module's own reference data.
 *
 * <p>Reports needs to know which stores make up a region in order to roll figures up. That mapping
 * is reporting reference data, so the module owns it rather than reaching into another module.
 */
public interface StoreDirectoryRepository {

  /** Store ids that belong to a region, empty when the region is unknown. */
  List<String> findStoreIdsByRegion(String regionId);
}
