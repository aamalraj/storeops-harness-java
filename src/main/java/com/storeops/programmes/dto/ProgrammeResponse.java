package com.storeops.programmes.dto;

import com.storeops.programmes.domain.Programme;
import com.storeops.programmes.domain.ProgrammeStatus;
import java.time.Instant;
import java.util.List;

/** Wire representation of a programme, including its membership. */
public record ProgrammeResponse(
    String id,
    String storeId,
    String name,
    ProgrammeStatus status,
    String ownerId,
    List<ProgrammeMemberResponse> members,
    Instant createdAt,
    Instant updatedAt) {

  public static ProgrammeResponse from(Programme programme) {
    return new ProgrammeResponse(
        programme.id(),
        programme.storeId(),
        programme.name(),
        programme.status(),
        programme.ownerId(),
        programme.members().stream().map(ProgrammeMemberResponse::from).toList(),
        programme.createdAt(),
        programme.updatedAt());
  }
}
