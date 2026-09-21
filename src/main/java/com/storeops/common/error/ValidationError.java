package com.storeops.common.error;

import java.util.List;

/** Raised when a request payload or filter combination is not acceptable. Answers HTTP 400. */
public class ValidationError extends AppError {

  private static final long serialVersionUID = 1L;

  private final List<String> details;

  public ValidationError(String message) {
    this(message, List.of());
  }

  public ValidationError(String message, List<String> details) {
    super("VALIDATION_FAILED", message, 400);
    this.details = List.copyOf(details);
  }

  public List<String> getDetails() {
    return details;
  }
}
