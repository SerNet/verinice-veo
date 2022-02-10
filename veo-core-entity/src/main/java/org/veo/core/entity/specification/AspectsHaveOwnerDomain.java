/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Element;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.aspects.Aspect;

/**
 * Checks that an aspect's domain is contained in its owner's domains.
 */
class AspectsHaveOwnerDomain implements EntitySpecification<Aspect> {

    @SuppressWarnings("rawtypes")
    @Override
    public boolean test(Aspect aspect) {
        Versioned owner = aspect.getOwner();
        if (owner instanceof Element) {
            return ((Element) owner).getDomains()
                                    .contains(aspect.getDomain());
        } else if (owner instanceof AbstractRisk) {
            return ((AbstractRisk) owner).getDomains()
                                         .contains(aspect.getDomain());
        }
        throw new IllegalArgumentException("Unable to check domains of aspect " + aspect
                + ", unhandled type " + aspect.getClass());
    }
}