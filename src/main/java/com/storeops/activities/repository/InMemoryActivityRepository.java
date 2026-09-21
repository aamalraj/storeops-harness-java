package com.storeops.activities.repository;

import com.storeops.activities.domain.Activity;
import com.storeops.activities.domain.ActivityStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory {@link ActivityRepository}.
 *
 * <p>Stub storage: a concurrent map keyed by activity id, no persistence across restarts.
 */
@Repository
public class InMemoryActivityRepository implements ActivityRepository {

  private final Map<String, Activity> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Activity> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Activity> findByStore(String storeId, String programmeId, ActivityStatus status) {
    return store.values().stream()
        .filter(activity -> activity.storeId().equals(storeId))
        .filter(activity -> programmeId == null
            || Objects.equals(activity.programmeId(), programmeId))
        .filter(activity -> status == null || activity.status() == status)
        .sorted(Comparator.comparing(Activity::createdAt).reversed()
            .thenComparing(Activity::id))
        .toList();
  }

  @Override
  public Activity save(Activity activity) {
    store.put(activity.id(), activity);
    return activity;
  }

  @Override
  public boolean deleteById(String id) {
    return store.remove(id) != null;
  }
}
