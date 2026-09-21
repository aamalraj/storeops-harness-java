package com.storeops.common.error;

/** Raised when a known path was called with an unsupported HTTP method. Answers HTTP 405. */
public class MethodNotAllowedError extends AppError {

  private static final long serialVersionUID = 1L;

  public MethodNotAllowedError(String method, String path) {
    super("METHOD_NOT_ALLOWED", method + " is not supported for " + path, 405);
  }
}
