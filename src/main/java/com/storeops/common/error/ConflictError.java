package com.storeops.common.error;

/** Raised when an action conflicts with the current state of a resource. Answers HTTP 409. */
public class ConflictError extends AppError {

  private static final long serialVersionUID = 1L;

  public ConflictError(String message) {
    super("CONFLICT", message, 409);
  }
}
