package com.storeops.programmes.repository;

import com.storeops.programmes.domain.Programme;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory {@link ProgrammeRepository}.
 *
 * <p>Stub storage: a concurrent map keyed by programme id, no persistence across restarts.
 */
@Repository
public class InMemoryProgrammeRepository implements ProgrammeRepository {

  private final Map<String, Programme> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Programme> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Programme> findByStoreId(String storeId) {
    return store.values().stream()
        .filter(programme -> programme.storeId().equals(storeId))
        .sorted(Comparator.comparing(Programme::name).thenComparing(Programme::id))
        .toList();
  }

  @Override
  public Programme save(Programme programme) {
    store.put(programme.id(), programme);
    return programme;
  }
}
