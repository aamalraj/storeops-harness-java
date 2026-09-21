package com.storeops.programmes.api;

import com.storeops.programmes.domain.ProgrammeStatus;
import com.storeops.programmes.service.ProgrammeService;
import org.springframework.stereotype.Component;

/** Adapts {@link ProgrammeService} reads to the cross-module metrics port. */
@Component
public class ProgrammeMetricsAdapter implements ProgrammeMetricsQuery {

  private final ProgrammeService programmeService;

  public ProgrammeMetricsAdapter(ProgrammeService programmeService) {
    this.programmeService = programmeService;
  }

  @Override
  public long countAll(String storeId) {
    return programmeService.countAll(storeId);
  }

  @Override
  public long countActive(String storeId) {
    return programmeService.countByStatus(storeId, ProgrammeStatus.ACTIVE);
  }
}
