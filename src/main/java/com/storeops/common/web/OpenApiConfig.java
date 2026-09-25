package com.storeops.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI / OpenAPI setup, served at {@code /swagger-ui.html} and {@code /v3/api-docs}.
 *
 * <p>Caller identity here is three plain headers, not a bearer token (see
 * {@link com.storeops.common.auth.ActorArgumentResolver}), so the security scheme below models
 * each header as its own API-key input. Setting them once via Swagger UI's "Authorize" button
 * applies them to every "Try it out" call. The resolved {@code Actor} parameter itself is hidden
 * from the schema via {@code @Parameter(hidden = true)} on
 * {@link com.storeops.common.auth.AuthenticatedActor}, not here — see that annotation's Javadoc.
 */
@Configuration
public class OpenApiConfig {

  private static final String USER_HEADER = "X-User-Id";
  private static final String STORE_HEADER = "X-Store-Id";
  private static final String ROLE_HEADER = "X-User-Role";

  @Bean
  public OpenAPI storeOpsOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("StoreOps API")
            .description("Retail store operations management REST API. Every request is scoped "
                + "to a store and an authenticated staff member via the headers below — there is "
                + "no login endpoint; a real deployment would have an upstream gateway forward "
                + "these after verifying the caller.")
            .version("v1"))
        .components(new Components()
            .addSecuritySchemes(USER_HEADER, headerScheme(USER_HEADER, "Calling staff member's "
                + "id, e.g. staff-1. Required."))
            .addSecuritySchemes(STORE_HEADER, headerScheme(STORE_HEADER, "Store the caller is "
                + "signed in to, e.g. store-1. Required — every query and mutation is scoped to "
                + "this store."))
            .addSecuritySchemes(ROLE_HEADER, headerScheme(ROLE_HEADER, "One of ASSOCIATE, "
                + "SUPERVISOR, STORE_MANAGER, REGION_MANAGER. Optional, defaults to ASSOCIATE.")))
        .addSecurityItem(new SecurityRequirement()
            .addList(USER_HEADER)
            .addList(STORE_HEADER)
            .addList(ROLE_HEADER));
  }

  private static SecurityScheme headerScheme(String headerName, String description) {
    return new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.HEADER)
        .name(headerName)
        .description(description);
  }
}
