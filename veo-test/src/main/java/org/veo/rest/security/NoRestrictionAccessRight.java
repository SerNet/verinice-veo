/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest.security;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.ClientOwned;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class NoRestrictionAccessRight implements UserAccessRights {
  private final String clientId;
  private final Integer maxUnits;

  @Override
  public void checkClient(ClientOwned id) {}

  @Override
  public boolean isUnitAccessRestricted() {
    return false;
  }

  @Override
  public Set<UUID> getReadableUnitIds() {
    return Collections.emptySet();
  }

  @Override
  public Set<UUID> getWritableUnitIds() {
    return Collections.emptySet();
  }

  @Override
  public List<String> getRoles() {
    return Collections.emptyList();
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  public static NoRestrictionAccessRight from(String clientId) {
    return new NoRestrictionAccessRight(clientId, 0);
  }

  public static NoRestrictionAccessRight from(String clientId, Integer maxUnits) {
    return new NoRestrictionAccessRight(clientId, maxUnits);
  }

  @Override
  public String getUsername() {
    return "System";
  }

  @Override
  public Integer getMaxUnits() {
    return maxUnits;
  }
}
