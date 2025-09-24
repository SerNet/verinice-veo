/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.persistence.entity.jpa;

import java.util.Objects;
import java.util.Set;

import org.veo.core.entity.FlyweightElement;
import org.veo.core.entity.FlyweightLink;

public record FlyweightElementData(String sourceId, Set<FlyweightLink> links)
    implements FlyweightElement {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FlyweightElementData that = (FlyweightElementData) o;
    return Objects.equals(sourceId, that.sourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(sourceId);
  }
}
