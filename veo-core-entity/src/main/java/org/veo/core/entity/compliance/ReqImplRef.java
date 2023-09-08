/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.core.entity.compliance;

import java.util.UUID;

import org.veo.core.entity.Key;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@EqualsAndHashCode
@AllArgsConstructor(staticName = "from", access = AccessLevel.PUBLIC)
@Value
public class ReqImplRef {
  @Getter String keyRef;

  public static ReqImplRef from(RequirementImplementation reqImpl) {
    return new ReqImplRef(reqImpl.getId().toString());
  }

  public UUID getUUID() {
    return UUID.fromString(keyRef);
  }

  public Key<UUID> toKey() {
    return new Key<>(getUUID());
  }

  public boolean references(RequirementImplementation reqImpl) {
    return reqImpl.getId().toString().equals(keyRef);
  }
}
