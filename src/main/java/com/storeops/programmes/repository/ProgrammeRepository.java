package com.storeops.programmes.repository;

import com.storeops.programmes.domain.Programme;
import java.util.List;
import java.util.Optional;

/** Persistence port for programmes. */
public interface ProgrammeRepository {

  Optional<Programme> findById(String id);

  List<Programme> findByStoreId(String storeId);

  Programme save(Programme programme);
}
