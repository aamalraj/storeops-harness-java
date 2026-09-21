package com.storeops.staff.dto;

import com.storeops.common.auth.ActorRole;
import com.storeops.staff.domain.StaffMember;

/** Wire representation of a staff member. */
public record StaffResponse(
    String id, String storeId, String regionId, String fullName, ActorRole role) {

  public static StaffResponse from(StaffMember member) {
    return new StaffResponse(
        member.id(), member.storeId(), member.regionId(), member.fullName(), member.role());
  }
}
