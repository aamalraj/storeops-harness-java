package com.storeops.staff.api;

import com.storeops.staff.domain.StaffMember;
import com.storeops.staff.service.StaffService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Exposes {@link StaffService} reads to other modules through the narrow directory port. */
@Component
public class StaffDirectoryAdapter implements StaffDirectory {

  private final StaffService staffService;

  public StaffDirectoryAdapter(StaffService staffService) {
    this.staffService = staffService;
  }

  @Override
  public Optional<StaffMember> findById(String staffId) {
    return staffService.findById(staffId);
  }

  @Override
  public List<StaffMember> listByStore(String storeId) {
    return staffService.listByStore(storeId);
  }

  @Override
  public boolean existsInStore(String staffId, String storeId) {
    return staffService.findById(staffId)
        .map(member -> member.storeId().equals(storeId))
        .orElse(false);
  }
}
