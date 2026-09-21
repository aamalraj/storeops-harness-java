package com.storeops.programmes.web;

import com.storeops.common.auth.Actor;
import com.storeops.common.auth.AuthenticatedActor;
import com.storeops.programmes.dto.AddMemberRequest;
import com.storeops.programmes.dto.CreateProgrammeRequest;
import com.storeops.programmes.dto.ProgrammeResponse;
import com.storeops.programmes.service.ProgrammeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Programme routes. Translates HTTP to and from {@link ProgrammeService}. */
@RestController
@RequestMapping("/api/programmes")
public class ProgrammeRoutes {

  private final ProgrammeService programmeService;

  public ProgrammeRoutes(ProgrammeService programmeService) {
    this.programmeService = programmeService;
  }

  /** GET /api/programmes — list programmes for the authenticated store. */
  @GetMapping
  public List<ProgrammeResponse> list(@AuthenticatedActor Actor actor) {
    return programmeService.listForActor(actor).stream().map(ProgrammeResponse::from).toList();
  }

  /** POST /api/programmes — create a new programme. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProgrammeResponse create(
      @AuthenticatedActor Actor actor, @Valid @RequestBody CreateProgrammeRequest request) {
    return ProgrammeResponse.from(programmeService.create(actor, request));
  }

  /** POST /api/programmes/{id}/members — add a staff member to a programme. */
  @PostMapping("/{id}/members")
  @ResponseStatus(HttpStatus.CREATED)
  public ProgrammeResponse addMember(
      @AuthenticatedActor Actor actor,
      @PathVariable String id,
      @Valid @RequestBody AddMemberRequest request) {
    return ProgrammeResponse.from(programmeService.addMember(actor, id, request));
  }
}
