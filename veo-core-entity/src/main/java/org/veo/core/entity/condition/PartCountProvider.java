/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.entity.condition;

import java.util.stream.Collectors;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Provides the amount of a composite element's parts. */
@AllArgsConstructor
@Data
public class PartCountProvider implements InputProvider {
  /** Define this to only count parts of a certain subtype */
  private final String partSubType;

  @Override
  public Object getValue(Element element, Domain domain) {
    if (element instanceof CompositeElement) {
      var parts = ((CompositeElement<?>) element).getParts();
      if (partSubType != null) {
        parts =
            parts.stream()
                .filter(
                    c ->
                        partSubType.equals(
                            // TODO VEO-1569: this fails if the part's subtypeAspects are a
                            // hibernate proxy
                            c.getSubType(domain).orElse(null)))
                .collect(Collectors.toSet());
      }
      return parts.size();
    }
    return 0;
  }
}
