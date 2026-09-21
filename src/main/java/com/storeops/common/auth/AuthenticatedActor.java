package com.storeops.common.auth;

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
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedActor {
}
