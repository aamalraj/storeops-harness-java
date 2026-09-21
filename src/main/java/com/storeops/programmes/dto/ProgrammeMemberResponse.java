package com.storeops.programmes.dto;

import com.storeops.programmes.domain.ProgrammeMember;
import com.storeops.programmes.domain.ProgrammeRole;
import java.time.Instant;

/** Wire representation of a programme membership. */
public record ProgrammeMemberResponse(String staffId, ProgrammeRole role, Instant addedAt) {

  public static ProgrammeMemberResponse from(ProgrammeMember member) {
    return new ProgrammeMemberResponse(member.staffId(), member.role(), member.addedAt());
  }
}
