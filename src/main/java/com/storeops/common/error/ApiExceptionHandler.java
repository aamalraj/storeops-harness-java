package com.storeops.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Renders the typed {@link AppError} hierarchy — and anything that escapes it — as a uniform
 * {@link ErrorResponse}. This is the single place in the application allowed to translate an
 * unexpected throwable into an HTTP response.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ValidationError.class)
  public ResponseEntity<ErrorResponse> onValidation(ValidationError error, HttpServletRequest req) {
    return render(error, error.getDetails(), req);
  }

  @ExceptionHandler(AppError.class)
  public ResponseEntity<ErrorResponse> onAppError(AppError error, HttpServletRequest req) {
    return render(error, List.of(), req);
  }

  /** Bean-validation failures on request bodies surface as the same VALIDATION_FAILED shape. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> onBeanValidation(
      MethodArgumentNotValidException exception, HttpServletRequest req) {
    List<String> details = exception.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .toList();
    return render(new ValidationError("Request payload is not valid", details), details, req);
  }

  /**
   * Framework-level request faults.
   *
   * <p>Malformed JSON, a missing query parameter or an unbindable path variable are all caller
   * mistakes, so they are folded into the same VALIDATION_FAILED shape rather than being allowed
   * to reach the 500 safety net below.
   */
  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorResponse> onMalformedRequest(
      Exception exception, HttpServletRequest req) {
    LOG.debug("Rejecting malformed request {} {}", req.getMethod(), req.getRequestURI(), exception);
    return render(new ValidationError("Request could not be read"), List.of(), req);
  }

  /** An unmapped path is a 404, not an internal error. */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> onNoHandler(
      NoHandlerFoundException exception, HttpServletRequest req) {
    return render(new NotFoundError("Route", req.getRequestURI()), List.of(), req);
  }

  /** A known path called with the wrong verb is a 405. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> onMethodNotAllowed(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest req) {
    AppError error = new MethodNotAllowedError(req.getMethod(), req.getRequestURI());
    return render(error, List.of(), req);
  }

  /** Safety net: never leak an internal stack trace or message to the caller. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> onUnexpected(Exception exception, HttpServletRequest req) {
    LOG.error("Unhandled exception for {} {}", req.getMethod(), req.getRequestURI(), exception);
    ErrorResponse body = new ErrorResponse(
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        500,
        List.of(),
        req.getRequestURI(),
        Instant.now());
    return ResponseEntity.status(500).body(body);
  }

  private static ResponseEntity<ErrorResponse> render(
      AppError error, List<String> details, HttpServletRequest req) {
    return ResponseEntity.status(error.getStatusCode())
        .body(ErrorResponse.of(error, details, req.getRequestURI()));
  }
}
