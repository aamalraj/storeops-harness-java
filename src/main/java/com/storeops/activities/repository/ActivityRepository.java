package com.storeops.activities.repository;

import com.storeops.activities.domain.Activity;
import com.storeops.activities.domain.ActivityStatus;
import java.util.List;
import java.util.Optional;

/** Persistence port for activities. */
public interface ActivityRepository {

  Optional<Activity> findById(String id);

  /**
   * Lists a store's activities newest first.
   *
   * @param storeId store to scope the query to, required
   * @param programmeId optional programme filter, null for no filter
   * @param status optional status filter, null for no filter
   */
  List<Activity> findByStore(String storeId, String programmeId, ActivityStatus status);

  Activity save(Activity activity);

  boolean deleteById(String id);
}
