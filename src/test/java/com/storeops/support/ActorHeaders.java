package com.storeops.support;

import org.springframework.http.HttpHeaders;

/**
 * Builds the caller identity headers that {@code ActorArgumentResolver} reads.
 *
 * <p>Store and staff ids match the seed data in {@code InMemoryStaffRepository}.
 */
public final class ActorHeaders {

  public static final String STORE = "store-1";
  public static final String OTHER_STORE = "store-2";
  public static final String REGION = "region-north";

  public static final String MANAGER = "staff-1";
  public static final String SUPERVISOR = "staff-2";
  public static final String ASSOCIATE = "staff-3";
  public static final String OTHER_STORE_ASSOCIATE = "staff-4";

  private ActorHeaders() {
  }

  /** Headers for a caller at the default store with the given user id and role. */
  public static HttpHeaders of(String userId, String role) {
    return of(userId, STORE, role);
  }

  /** Headers for a caller at an explicit store. */
  public static HttpHeaders of(String userId, String storeId, String role) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-User-Id", userId);
    headers.set("X-Store-Id", storeId);
    headers.set("X-Region-Id", REGION);
    headers.set("X-User-Role", role);
    return headers;
  }

  /** Headers for an ordinary associate at the default store. */
  public static HttpHeaders associate() {
    return of(ASSOCIATE, "ASSOCIATE");
  }

  /** Headers for a store manager at the default store. */
  public static HttpHeaders manager() {
    return of(MANAGER, "STORE_MANAGER");
  }
}
