package com.storeops.staff.service;

import com.storeops.common.auth.Actor;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.staff.domain.StaffMember;
import com.storeops.staff.repository.StaffRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Staff read model.
 *
 * <p>Stub implementation. Note the absence of mutating operations: staff records are maintained by
 * the HR system of record, so StoreOps only ever reads them.
 */
@Service
public class StaffService {

  private final StaffRepository repository;

  public StaffService(StaffRepository repository) {
    this.repository = repository;
  }

  /** Looks up a staff member without any scope restriction, for internal callers. */
  public Optional<StaffMember> findById(String staffId) {
    return repository.findById(staffId).filter(StaffMember::active);
  }

  /** Looks up a staff member, failing with 404 when absent. */
  public StaffMember getById(String staffId) {
    return findById(staffId).orElseThrow(() -> new NotFoundError("Staff member", staffId));
  }

  /** Lists the active staff of a store. */
  public List<StaffMember> listByStore(String storeId) {
    return repository.findByStoreId(storeId).stream().filter(StaffMember::active).toList();
  }

  /** Lists staff visible to {@code actor}, which is the store the actor is signed in to. */
  public List<StaffMember> listForActor(Actor actor, String requestedStoreId) {
    String storeId = requestedStoreId == null ? actor.storeId() : requestedStoreId;
    if (!storeId.equals(actor.storeId()) && !actor.role().isManager()) {
      throw new ForbiddenError("Only managers may read staff outside their own store");
    }
    return listByStore(storeId);
  }
}
