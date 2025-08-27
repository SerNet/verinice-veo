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
package org.veo.core;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Unit;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.NotAllowedException;
import org.veo.core.entity.state.ElementState;

public interface UserAccessRights {
  String UNIT_ACCESS_RESTRICTION = "unit_access_restriction";
  String READ_WRITE_ALL_UNITS = "read_write_all_units";

  Set<UUID> getReadableUnitIds();

  Set<UUID> getWritableUnitIds();

  List<String> getRoles();

  String getClientId();

  String getUsername();

  default boolean isUnitAccessRestricted() {
    if (getRoles().contains(READ_WRITE_ALL_UNITS)) return false;
    return getRoles().contains(UNIT_ACCESS_RESTRICTION);
  }

  default void checkUnitCreateAllowed() {
    if (isUnitAccessRestricted()) {
      if (!getRoles().contains("unit:create")) {
        throw new NotAllowedException("Missing unit:create permission.");
      }
    }
  }

  default void checkUnitDeleteAllowed() {
    if (isUnitAccessRestricted()) {
      if (!getRoles().contains("unit:delete")) {
        throw new NotAllowedException("Missing unit:delete permission.");
      }
    }
  }

  default void checkUnitUpdateAllowed() {
    if (isUnitAccessRestricted()) {
      if (!getRoles().contains("unit:update")) {
        throw new NotAllowedException("Missing unit:update permission.");
      }
    }
  }

  default void checkCreateElementWriteAccess(ElementState<?> element) {
    if (isUnitAccessRestricted()) {
      if (!getWritableUnitIds().contains(element.getOwner().getId())) {
        throw new NotAllowedException(
            "Missing unit '%s' write permission.".formatted(element.getOwner().getId().toString()));
      }
    }
  }

  default void checkElementWriteAccess(Element element) {
    checkElementWriteAccess(element.getOwner());
  }

  default void checkElementWriteAccess(Unit unit) {
    checkClient(unit);
    if (isUnitAccessRestricted()) {
      if (!getWritableUnitIds().contains(unit.getId())) {
        throw new NotAllowedException(
            "Missing unit '%s' write permission.".formatted(unit.getName()));
      }
    }
  }

  default void checkClient(ClientOwned co) {
    if (co.getOwningClient().isPresent()) {
      if (!co.getOwningClient().get().getId().equals(clientId())) {
        throw new ClientBoundaryViolationException((Identifiable) co, clientId());
      }
    }
  }

  default UUID clientId() {
    return UUID.fromString(getClientId());
  }

  class AnonymousUser implements UserAccessRights {

    @Override
    public Set<UUID> getReadableUnitIds() {
      return Set.of();
    }

    @Override
    public Set<UUID> getWritableUnitIds() {
      return Set.of();
    }

    @Override
    public List<String> getRoles() {
      return List.of();
    }

    @Override
    public String getClientId() {
      return null;
    }

    @Override
    public String getUsername() {
      return null;
    }
  }
}
