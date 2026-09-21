package com.storeops.staff.repository;

import com.storeops.common.auth.ActorRole;
import com.storeops.staff.domain.StaffMember;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory {@link StaffRepository}.
 *
 * <p>Stub storage seeded with a handful of members so the other modules have real ids to reference
 * when the application is run by hand.
 */
@Repository
public class InMemoryStaffRepository implements StaffRepository {

  private final Map<String, StaffMember> store = new ConcurrentHashMap<>();

  @PostConstruct
  void seed() {
    save(new StaffMember("staff-1", "store-1", "region-north", "Ada Okafor",
        ActorRole.STORE_MANAGER, true));
    save(new StaffMember("staff-2", "store-1", "region-north", "Ben Sato",
        ActorRole.SUPERVISOR, true));
    save(new StaffMember("staff-3", "store-1", "region-north", "Cleo Marsh",
        ActorRole.ASSOCIATE, true));
    save(new StaffMember("staff-4", "store-2", "region-north", "Dev Patel",
        ActorRole.ASSOCIATE, true));
  }

  @Override
  public Optional<StaffMember> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<StaffMember> findByStoreId(String storeId) {
    return store.values().stream()
        .filter(member -> member.storeId().equals(storeId))
        .sorted(Comparator.comparing(StaffMember::fullName))
        .toList();
  }

  @Override
  public StaffMember save(StaffMember member) {
    store.put(member.id(), member);
    return member;
  }
}
