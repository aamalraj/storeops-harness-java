package com.storeops.common.auth;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a route parameter that should be populated with the calling {@link Actor}.
 *
 * <p>Resolved by {@link ActorArgumentResolver}; a request without caller headers is rejected with
 * HTTP 401 before the route body runs.
 *
 * <p>Meta-annotated with {@code @Parameter(hidden = true)} so springdoc — which has no way to
 * know this argument comes from a header-backed resolver rather than the request — omits it from
 * the generated OpenAPI schema instead of rendering it as a bogus request parameter.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface AuthenticatedActor {
}
