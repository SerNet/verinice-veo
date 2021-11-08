/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;

/**
 * A domain must contain a definition for each supported element type.
 */
public class CompleteEntityTypeDefinitionsSpecification implements EntitySpecification<Domain> {
    @Override
    public boolean test(Domain entity) {
        return EntityType.ELEMENT_TYPES.stream()
                                       .allMatch(t -> entity.getElementTypeDefinition(t.getSingularTerm())
                                                            .isPresent());
    }
}
