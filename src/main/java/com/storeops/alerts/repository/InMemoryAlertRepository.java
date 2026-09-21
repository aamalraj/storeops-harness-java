package com.storeops.alerts.repository;

import com.storeops.alerts.domain.Alert;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory {@link AlertRepository}.
 *
 * <p>Stub storage: a concurrent map keyed by alert id, no persistence across restarts.
 */
@Repository
public class InMemoryAlertRepository implements AlertRepository {

  private final Map<String, Alert> store = new ConcurrentHashMap<>();

  @Override
  public List<Alert> findByRecipient(String recipientId, boolean unacknowledgedOnly) {
    return store.values().stream()
        .filter(alert -> alert.recipientId().equals(recipientId))
        .filter(alert -> !unacknowledgedOnly || !alert.acknowledged())
        .sorted(Comparator.comparing(Alert::createdAt).reversed().thenComparing(Alert::id))
        .toList();
  }

  @Override
  public Alert save(Alert alert) {
    store.put(alert.id(), alert);
    return alert;
  }
}
