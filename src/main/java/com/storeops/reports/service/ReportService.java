package com.storeops.reports.service;

import com.storeops.activities.api.ActivityMetricsQuery;
import com.storeops.common.auth.Actor;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.programmes.api.ProgrammeMetricsQuery;
import com.storeops.reports.domain.RegionMetrics;
import com.storeops.reports.domain.StoreMetrics;
import com.storeops.reports.repository.StoreDirectoryRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Store and region metric use cases.
 *
 * <p>Stub implementation: figures are recomputed on every request from the read-only query ports
 * the activities and programmes modules publish. Reports is a pure consumer — no other module
 * depends on it, which is what keeps the dependency graph acyclic.
 */
@Service
public class ReportService {

  private final ActivityMetricsQuery activityMetrics;
  private final ProgrammeMetricsQuery programmeMetrics;
  private final StoreDirectoryRepository storeDirectory;

  public ReportService(
      ActivityMetricsQuery activityMetrics,
      ProgrammeMetricsQuery programmeMetrics,
      StoreDirectoryRepository storeDirectory) {
    this.activityMetrics = activityMetrics;
    this.programmeMetrics = programmeMetrics;
    this.storeDirectory = storeDirectory;
  }

  /** Metrics for one store; non-managers may only read their own store. */
  public StoreMetrics storeReport(Actor actor, String requestedStoreId) {
    String storeId = requestedStoreId == null || requestedStoreId.isBlank()
        ? actor.storeId()
        : requestedStoreId.trim();
    if (!storeId.equals(actor.storeId()) && !actor.role().isManager()) {
      throw new ForbiddenError("Only managers may read metrics for another store");
    }
    return metricsFor(storeId);
  }

  /** Roll-up for a region; managers only. */
  public RegionMetrics regionReport(Actor actor, String requestedRegionId) {
    if (!actor.role().isManager()) {
      throw new ForbiddenError("Only managers may read region metrics");
    }
    String regionId = requestedRegionId == null || requestedRegionId.isBlank()
        ? actor.regionId()
        : requestedRegionId.trim();

    List<String> storeIds = storeDirectory.findStoreIdsByRegion(regionId);
    if (storeIds.isEmpty()) {
      throw new NotFoundError("Region", regionId);
    }

    List<StoreMetrics> stores = storeIds.stream().map(this::metricsFor).toList();
    return new RegionMetrics(
        regionId,
        stores,
        stores.stream().mapToLong(StoreMetrics::openActivities).sum(),
        stores.stream().mapToLong(StoreMetrics::activeProgrammes).sum(),
        Instant.now());
  }

  private StoreMetrics metricsFor(String storeId) {
    return new StoreMetrics(
        storeId,
        activityMetrics.countByStatus(storeId),
        activityMetrics.countOpen(storeId),
        programmeMetrics.countAll(storeId),
        programmeMetrics.countActive(storeId),
        Instant.now());
  }
}
