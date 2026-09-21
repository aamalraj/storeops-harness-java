package com.storeops.reports.web;

import com.storeops.common.auth.Actor;
import com.storeops.common.auth.AuthenticatedActor;
import com.storeops.reports.domain.RegionMetrics;
import com.storeops.reports.domain.StoreMetrics;
import com.storeops.reports.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report routes.
 *
 * <p>Neither route is one of the nine endpoints in the API specification — they exist so the
 * reports module has the same Routes → Service → Repository shape as the rest.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportRoutes {

  private final ReportService reportService;

  public ReportRoutes(ReportService reportService) {
    this.reportService = reportService;
  }

  /** GET /api/reports/store — metrics for the authenticated store. */
  @GetMapping("/store")
  public StoreMetrics store(
      @AuthenticatedActor Actor actor,
      @RequestParam(name = "storeId", required = false) String storeId) {
    return reportService.storeReport(actor, storeId);
  }

  /** GET /api/reports/region — roll-up across a region, managers only. */
  @GetMapping("/region")
  public RegionMetrics region(
      @AuthenticatedActor Actor actor,
      @RequestParam(name = "regionId", required = false) String regionId) {
    return reportService.regionReport(actor, regionId);
  }
}
