package com.storeops.staff.api;

import com.storeops.staff.domain.StaffMember;
import java.util.List;
import java.util.Optional;

/**
 * The read-only view of staff that other modules are allowed to depend on.
 *
 * <p>Module boundary rule: staff is read-only for other modules. Only query methods appear here —
 * there is deliberately no create/update/delete — and the ArchUnit module-boundary test forbids any
 * module outside {@code com.storeops.staff} from reaching the staff service, repository or routes
 * directly.
 */
public interface StaffDirectory {

  /** Looks up one staff member. */
  Optional<StaffMember> findById(String staffId);

  /** Lists the active staff of a store. */
  List<StaffMember> listByStore(String storeId);

  /** True when the staff member exists and is active at {@code storeId}. */
  boolean existsInStore(String staffId, String storeId);
}
