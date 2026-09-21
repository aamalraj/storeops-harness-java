package com.storeops.reports.repository;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * In-memory {@link StoreDirectoryRepository}.
 *
 * <p>Stub storage seeded with one region so region roll-ups return something when the application
 * is run by hand.
 */
@Repository
public class InMemoryStoreDirectoryRepository implements StoreDirectoryRepository {

  private static final Map<String, List<String>> STORES_BY_REGION =
      Map.of("region-north", List.of("store-1", "store-2"));

  @Override
  public List<String> findStoreIdsByRegion(String regionId) {
    return STORES_BY_REGION.getOrDefault(regionId, List.of());
  }
}
