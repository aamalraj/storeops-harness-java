package com.storeops.common.auth;

import com.storeops.common.error.UnauthorizedError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Actor} arguments from request headers.
 *
 * <p>Stub authentication: an upstream gateway is assumed to have verified the caller and to forward
 * the identity headers below. Swapping this for a real token verifier is the only change needed —
 * no route or service touches the headers directly.
 */
@Component
public class ActorArgumentResolver implements HandlerMethodArgumentResolver {

  static final String USER_HEADER = "X-User-Id";
  static final String STORE_HEADER = "X-Store-Id";
  static final String REGION_HEADER = "X-Region-Id";
  static final String ROLE_HEADER = "X-User-Role";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(AuthenticatedActor.class)
        && Actor.class.equals(parameter.getParameterType());
  }

  @Override
  public Actor resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    if (request == null) {
      throw new UnauthorizedError("Caller identity is unavailable");
    }
    String userId = required(request, USER_HEADER);
    String storeId = required(request, STORE_HEADER);
    String regionId = header(request, REGION_HEADER, "region-unknown");
    return new Actor(userId, storeId, regionId, parseRole(header(request, ROLE_HEADER, null)));
  }

  private static String required(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    if (value == null || value.isBlank()) {
      throw new UnauthorizedError("Missing required header " + name);
    }
    return value.trim();
  }

  private static String header(HttpServletRequest request, String name, String fallback) {
    String value = request.getHeader(name);
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static ActorRole parseRole(String raw) {
    if (raw == null) {
      return ActorRole.ASSOCIATE;
    }
    try {
      return ActorRole.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException unknownRole) {
      throw new UnauthorizedError("Unknown role '" + raw + "'");
    }
  }
}
