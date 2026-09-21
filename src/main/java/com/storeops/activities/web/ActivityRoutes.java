package com.storeops.activities.web;

import com.storeops.activities.dto.ActivityResponse;
import com.storeops.activities.dto.CreateActivityRequest;
import com.storeops.activities.dto.UpdateActivityRequest;
import com.storeops.activities.service.ActivityService;
import com.storeops.common.auth.Actor;
import com.storeops.common.auth.AuthenticatedActor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Activity routes.
 *
 * <p>The route layer only translates HTTP to and from the service layer: it resolves the caller,
 * binds the payload and maps domain records to DTOs. Authorisation and validation live in
 * {@link ActivityService}, and errors travel as the typed {@code AppError} hierarchy.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityRoutes {

  private final ActivityService activityService;

  public ActivityRoutes(ActivityService activityService) {
    this.activityService = activityService;
  }

  /** GET /api/activities — list activities, optional programme and status filters. */
  @GetMapping
  public List<ActivityResponse> list(
      @AuthenticatedActor Actor actor,
      @RequestParam(name = "programme", required = false) String programme,
      @RequestParam(name = "status", required = false) String status) {
    return activityService.list(actor, programme, status).stream()
        .map(ActivityResponse::from)
        .toList();
  }

  /** POST /api/activities — create a new activity. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ActivityResponse create(
      @AuthenticatedActor Actor actor, @Valid @RequestBody CreateActivityRequest request) {
    return ActivityResponse.from(activityService.create(actor, request));
  }

  /** GET /api/activities/{id} — get activity by id. */
  @GetMapping("/{id}")
  public ActivityResponse getById(@AuthenticatedActor Actor actor, @PathVariable String id) {
    return ActivityResponse.from(activityService.getById(actor, id));
  }

  /** PATCH /api/activities/{id} — update status, priority, category and/or assignee. */
  @PatchMapping("/{id}")
  public ActivityResponse update(
      @AuthenticatedActor Actor actor,
      @PathVariable String id,
      @RequestBody UpdateActivityRequest request) {
    return ActivityResponse.from(activityService.update(actor, id, request));
  }

  /** DELETE /api/activities/{id} — owner or store manager only. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@AuthenticatedActor Actor actor, @PathVariable String id) {
    activityService.delete(actor, id);
    return ResponseEntity.noContent().build();
  }
}
