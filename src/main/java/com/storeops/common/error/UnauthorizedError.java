package com.storeops.common.error;

/** Raised when the caller could not be identified. Answers HTTP 401. */
public class UnauthorizedError extends AppError {

  private static final long serialVersionUID = 1L;

  public UnauthorizedError(String message) {
    super("UNAUTHORIZED", message, 401);
  }
}
