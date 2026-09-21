package com.storeops.common.auth;

/** Roles a StoreOps caller can hold, ordered from least to most privileged. */
public enum ActorRole {
  ASSOCIATE,
  SUPERVISOR,
  STORE_MANAGER,
  REGION_MANAGER;

  /** True when this role is allowed to act on records it does not own. */
  public boolean isManager() {
    return this == STORE_MANAGER || this == REGION_MANAGER;
  }
}
