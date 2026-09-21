package com.storeops.staff.web;

import com.storeops.common.auth.Actor;
import com.storeops.common.auth.AuthenticatedActor;
import com.storeops.staff.dto.StaffResponse;
import com.storeops.staff.service.StaffService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff routes.
 *
 * <p>Read-only by design: the staff module offers no write endpoint, to either HTTP callers or
 * other modules. This route is not one of the nine endpoints in the API specification — it exists
 * so the staff module has the same Routes → Service → Repository shape as the rest.
 */
@RestController
@RequestMapping("/api/staff")
public class StaffRoutes {

  private final StaffService staffService;

  public StaffRoutes(StaffService staffService) {
    this.staffService = staffService;
  }

  /** GET /api/staff — list staff for the authenticated store. */
  @GetMapping
  public List<StaffResponse> list(
      @AuthenticatedActor Actor actor,
      @RequestParam(name = "storeId", required = false) String storeId) {
    return staffService.listForActor(actor, storeId).stream().map(StaffResponse::from).toList();
  }
}
