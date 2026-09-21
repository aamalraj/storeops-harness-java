package com.storeops.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Wire format for every failed request.
 *
 * @param code stable machine-readable error code from the {@link AppError} hierarchy
 * @param message human-readable description
 * @param status HTTP status code that accompanies the response
 * @param details optional field-level messages, empty when there are none
 * @param path request path that produced the error
 * @param timestamp moment the error was rendered
 */
public record ErrorResponse(
    String code,
    String message,
    int status,
    List<String> details,
    String path,
    Instant timestamp) {

  public static ErrorResponse of(AppError error, List<String> details, String path) {
    return new ErrorResponse(
        error.getCode(),
        error.getMessage(),
        error.getStatusCode(),
        List.copyOf(details),
        path,
        Instant.now());
  }
}
