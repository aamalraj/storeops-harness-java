package com.storeops.common.error;

/** Raised when an addressed resource does not exist. Answers HTTP 404. */
public class NotFoundError extends AppError {

  private static final long serialVersionUID = 1L;

  public NotFoundError(String resource, String id) {
    super("NOT_FOUND", resource + " '" + id + "' was not found", 404);
  }
}
