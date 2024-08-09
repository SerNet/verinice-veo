/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.adapter.presenter.api.response;

import java.util.UUID;

import org.veo.adapter.presenter.api.DeviatingIdException;

public interface IdentifiableDto {
  UUID getId();

  void setId(UUID id);

  /**
   * Applies resource ID from resource location to this DTO.
   *
   * @throws DeviatingIdException if this DTO already has an ID that deviates from given resource
   *     ID.
   */
  default void applyResourceId(UUID resourceId) {
    var dtoId = getId();
    if (dtoId != null && !dtoId.equals(resourceId)) {
      throw new DeviatingIdException(
          String.format("DTO ID %s does not match resource ID %s", dtoId, resourceId));
    }
    setId(resourceId);
  }
}
