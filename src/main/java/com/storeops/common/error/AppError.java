package com.storeops.common.error;

/**
 * Base class for every error raised by StoreOps services and routes.
 *
 * <p>Services and routes must never throw a raw {@code RuntimeException}: each failure carries a
 * stable machine-readable {@link #getCode() code}, a human-readable message and the HTTP
 * {@link #getStatusCode() status} the API should answer with. Both the Checkstyle ruleset and the
 * ArchUnit module-boundary test fail the build when a raw throwable is constructed in a service or
 * route.
 */
public abstract class AppError extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final int statusCode;

  protected AppError(String code, String message, int statusCode) {
    super(message);
    this.code = code;
    this.statusCode = statusCode;
  }

  protected AppError(String code, String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.statusCode = statusCode;
  }

  public String getCode() {
    return code;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
