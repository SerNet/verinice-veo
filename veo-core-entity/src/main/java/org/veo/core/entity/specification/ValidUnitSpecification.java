/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;

/**
 * Checks if the given entity has a valid unit. This means that
 *
 * <pre>
 * - the unit is not null
 * - the unit must not have an undefined Key as ID
 * - the unit's name is not null and not an empty string.
 * </pre>
 *
 * @author akoderman
 *
 * @param <T>
 */
final class ValidUnitSpecification<T extends EntityLayerSupertype>
        implements EntitySpecification<T> {

    private ValidUnitSpecification() {
    }

    static final ValidUnitSpecification<EntityLayerSupertype> INSTANCE = new ValidUnitSpecification<>();

    @Override
    public boolean test(EntityLayerSupertype entity) {
        return isSatisfiedBy(entity.getOwner());
    }

    public boolean isSatisfiedBy(Unit unit) {
        return (unit != null && !unit.getId()
                                     .isUndefined()
                && unit.getName() != null && !unit.getName()
                                                  .isEmpty());
    }

}
