package com.storeops.alerts.repository;

import com.storeops.alerts.domain.Alert;
import java.util.List;

/** Persistence port for alerts. */
public interface AlertRepository {

  /** Lists a recipient's alerts newest first, optionally only the unacknowledged ones. */
  List<Alert> findByRecipient(String recipientId, boolean unacknowledgedOnly);

  Alert save(Alert alert);
}
