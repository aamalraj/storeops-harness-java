package com.storeops.common.util;

import com.storeops.common.error.ValidationError;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Parses caller-supplied strings into enums.
 *
 * <p>Request DTOs take enum-valued fields as strings so that an unknown value becomes a typed
 * {@link ValidationError} (HTTP 400, naming the accepted values) rather than a deserialisation
 * failure that would surface as a 500.
 */
public final class Enums {

  private Enums() {
  }

  /** Parses {@code raw}, or fails with a 400 naming the accepted values. */
  public static <E extends Enum<E>> E parse(Class<E> type, String raw, String field) {
    return parseOptional(type, raw, field)
        .orElseThrow(() -> new ValidationError(field + " is required"));
  }

  /** Parses {@code raw} when present; blank and null yield an empty result. */
  public static <E extends Enum<E>> Optional<E> parseOptional(
      Class<E> type, String raw, String field) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    String candidate = raw.trim().toUpperCase(Locale.ROOT);
    return Arrays.stream(type.getEnumConstants())
        .filter(value -> value.name().equals(candidate))
        .findFirst()
        .or(() -> {
          throw new ValidationError(
              field + " must be one of " + accepted(type) + " but was '" + raw + "'");
        });
  }

  private static <E extends Enum<E>> String accepted(Class<E> type) {
    return Arrays.stream(type.getEnumConstants())
        .map(Enum::name)
        .collect(Collectors.joining(", ", "[", "]"));
  }
}
