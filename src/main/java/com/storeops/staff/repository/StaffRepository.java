package com.storeops.staff.repository;

import com.storeops.staff.domain.StaffMember;
import java.util.List;
import java.util.Optional;

/** Persistence port for staff records. */
public interface StaffRepository {

  Optional<StaffMember> findById(String id);

  List<StaffMember> findByStoreId(String storeId);

  StaffMember save(StaffMember member);
}
