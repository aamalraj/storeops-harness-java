package com.storeops.common.error;

/** Raised when an identified caller is not allowed to perform the action. Answers HTTP 403. */
public class ForbiddenError extends AppError {

  private static final long serialVersionUID = 1L;

  public ForbiddenError(String message) {
    super("FORBIDDEN", message, 403);
  }
}
