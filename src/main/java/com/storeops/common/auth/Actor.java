package com.storeops.common.auth;

/**
 * The authenticated caller behind a request.
 *
 * <p>Routes receive this as a resolved argument (see {@link AuthenticatedActor}) and pass it down
 * to services, which use it for ownership and scope checks. Services never reach for ambient
 * request state of their own.
 *
 * @param userId identifier of the calling staff member
 * @param storeId store the caller is signed in to
 * @param regionId region the store belongs to
 * @param role the caller's role at that store
 */
public record Actor(String userId, String storeId, String regionId, ActorRole role) {

  /** True when {@code ownerId} is this caller. */
  public boolean owns(String ownerId) {
    return userId.equals(ownerId);
  }
}
