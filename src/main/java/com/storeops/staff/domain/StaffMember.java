package com.storeops.staff.domain;

import com.storeops.common.auth.ActorRole;

/**
 * A member of a store's staff.
 *
 * <p>Owned exclusively by the staff module. Other modules read staff through
 * {@link com.storeops.staff.api.StaffDirectory} and never mutate it.
 *
 * @param id staff member identifier, also the actor id used for ownership checks
 * @param storeId store the member is assigned to
 * @param regionId region the store belongs to
 * @param fullName display name
 * @param role role held at the store
 * @param active false once the member has left the store
 */
public record StaffMember(
    String id, String storeId, String regionId, String fullName, ActorRole role, boolean active) {
}
