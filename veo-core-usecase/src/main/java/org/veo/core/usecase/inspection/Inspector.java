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
package org.veo.core.usecase.inspection;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.inspection.Finding;

/** Runs all applicable inspections on an element (in the context of a domain). */
public class Inspector {
  public Set<Finding> inspect(Element element, Domain domain) {
    return domain.getInspections().values().stream()
        .map(inspection -> inspection.run(element, domain))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }
}
