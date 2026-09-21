package com.storeops.activities.api;

import com.storeops.activities.service.ActivityService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Adapts {@link ActivityService} reads to the cross-module metrics port. */
@Component
public class ActivityMetricsAdapter implements ActivityMetricsQuery {

  private final ActivityService activityService;

  public ActivityMetricsAdapter(ActivityService activityService) {
    this.activityService = activityService;
  }

  @Override
  public Map<String, Long> countByStatus(String storeId) {
    Map<String, Long> counts = new LinkedHashMap<>();
    activityService.countByStatus(storeId)
        .forEach((status, count) -> counts.put(status.name(), count));
    return counts;
  }

  @Override
  public long countOpen(String storeId) {
    return activityService.countOpen(storeId);
  }
}
