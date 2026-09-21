package com.storeops.alerts.web;

import com.storeops.alerts.dto.AlertResponse;
import com.storeops.alerts.service.AlertService;
import com.storeops.common.auth.Actor;
import com.storeops.common.auth.AuthenticatedActor;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Alert routes. */
@RestController
@RequestMapping("/api/alerts")
public class AlertRoutes {

  private final AlertService alertService;

  public AlertRoutes(AlertService alertService) {
    this.alertService = alertService;
  }

  /** GET /api/alerts — alerts for the authenticated user. */
  @GetMapping
  public List<AlertResponse> list(
      @AuthenticatedActor Actor actor,
      @RequestParam(name = "unacknowledgedOnly", defaultValue = "false") boolean unacknowledgedOnly) {
    return alertService.listForActor(actor, unacknowledgedOnly).stream()
        .map(AlertResponse::from)
        .toList();
  }
}
