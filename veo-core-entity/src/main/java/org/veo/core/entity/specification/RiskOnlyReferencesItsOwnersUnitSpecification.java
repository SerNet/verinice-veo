/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.entity.specification;

import java.util.Objects;
import java.util.stream.Stream;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Element;

public class RiskOnlyReferencesItsOwnersUnitSpecification
    implements EntitySpecification<AbstractRisk> {
  @Override
  public boolean test(AbstractRisk entity) {
    return Stream.of(entity.getScenario(), entity.getRiskOwner(), entity.getMitigation())
        .filter(Objects::nonNull)
        .map(Element::getOwner)
        .allMatch(u -> u.equals(entity.getEntity().getOwner()));
  }
}
