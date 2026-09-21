package com.storeops.programmes.service;

import com.storeops.common.auth.Actor;
import com.storeops.common.error.ConflictError;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.ValidationError;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.DomainEventType;
import com.storeops.common.events.EventBus;
import com.storeops.common.util.Enums;
import com.storeops.programmes.domain.Programme;
import com.storeops.programmes.domain.ProgrammeMember;
import com.storeops.programmes.domain.ProgrammeRole;
import com.storeops.programmes.domain.ProgrammeStatus;
import com.storeops.programmes.dto.AddMemberRequest;
import com.storeops.programmes.dto.CreateProgrammeRequest;
import com.storeops.programmes.repository.ProgrammeRepository;
import com.storeops.staff.api.StaffDirectory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Store programme use cases.
 *
 * <p>Stub implementation. Reads staff through the read-only {@link StaffDirectory} port and
 * notifies other modules only via the {@link EventBus}.
 */
@Service
public class ProgrammeService {

  private final ProgrammeRepository repository;
  private final StaffDirectory staffDirectory;
  private final EventBus eventBus;

  public ProgrammeService(
      ProgrammeRepository repository, StaffDirectory staffDirectory, EventBus eventBus) {
    this.repository = repository;
    this.staffDirectory = staffDirectory;
    this.eventBus = eventBus;
  }

  /** Lists the programmes of the caller's store. */
  public List<Programme> listForActor(Actor actor) {
    return repository.findByStoreId(actor.storeId());
  }

  /** Creates a programme owned by the caller. */
  public Programme create(Actor actor, CreateProgrammeRequest request) {
    ProgrammeStatus status =
        Enums.parseOptional(ProgrammeStatus.class, request.status(), "status")
            .orElse(ProgrammeStatus.DRAFT);
    if (status == ProgrammeStatus.ARCHIVED) {
      throw new ValidationError("A new programme cannot start out ARCHIVED");
    }

    Instant now = Instant.now();
    Programme programme = new Programme(
        UUID.randomUUID().toString(),
        actor.storeId(),
        request.name().trim(),
        status,
        actor.userId(),
        List.of(),
        now,
        now);

    Programme saved = repository.save(programme);
    eventBus.publish(DomainEvent.of(DomainEventType.PROGRAMME_CREATED, saved.id(),
        saved.storeId(), actor.userId(), Map.of("name", saved.name())));
    return saved;
  }

  /** Reads one programme, enforcing store scope. */
  public Programme getById(Actor actor, String id) {
    Programme programme = repository.findById(id)
        .orElseThrow(() -> new NotFoundError("Programme", id));
    if (!programme.storeId().equals(actor.storeId())) {
      throw new NotFoundError("Programme", id);
    }
    return programme;
  }

  /** Adds a staff member to a programme. */
  public Programme addMember(Actor actor, String programmeId, AddMemberRequest request) {
    Programme programme = getById(actor, programmeId);
    if (programme.isClosed()) {
      throw new ConflictError("Programme '" + programmeId + "' is " + programme.status()
          + " and cannot take new members");
    }
    if (!actor.owns(programme.ownerId()) && !actor.role().isManager()) {
      throw new ForbiddenError(
          "Only the programme owner or a store manager may add members");
    }

    String staffId = request.staffId().trim();
    if (!staffDirectory.existsInStore(staffId, programme.storeId())) {
      throw new ValidationError("Staff member '" + staffId + "' is not a member of this store");
    }
    if (programme.hasMember(staffId)) {
      throw new ConflictError("Staff member '" + staffId + "' is already on this programme");
    }

    ProgrammeRole role = Enums.parseOptional(ProgrammeRole.class, request.role(), "role")
        .orElse(ProgrammeRole.CONTRIBUTOR);
    Instant now = Instant.now();
    Programme saved = repository.save(
        programme.withMember(new ProgrammeMember(staffId, role, now), now));

    eventBus.publish(DomainEvent.of(DomainEventType.PROGRAMME_MEMBER_ADDED, saved.id(),
        saved.storeId(), actor.userId(), Map.of("staffId", staffId, "role", role.name())));
    return saved;
  }

  /** Read model used by the reports module through the programmes query port. */
  public long countByStatus(String storeId, ProgrammeStatus status) {
    return repository.findByStoreId(storeId).stream()
        .filter(programme -> programme.status() == status)
        .count();
  }

  /** Total programmes at a store, for the reports module. */
  public long countAll(String storeId) {
    return repository.findByStoreId(storeId).size();
  }
}
