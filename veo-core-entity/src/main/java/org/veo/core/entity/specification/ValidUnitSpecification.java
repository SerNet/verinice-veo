/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.core.entity.specification;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;

public class ValidUnitSpecification implements IEntitySpecification<EntityLayerSupertype>{

    @Override
    public boolean isSatisfiedBy(EntityLayerSupertype entity) {
        return isSatisfiedBy(entity.getUnit());
    }

    @Override
    public Set<EntityLayerSupertype> selectSatisfyingElementsFrom(
            Collection<EntityLayerSupertype> collection) {
        return collection.stream()
                .filter(this::isSatisfiedBy)
                .collect(Collectors.toSet());
    }

    public boolean isSatisfiedBy(Unit unit) {
        return (unit != null
                && !unit.getId().isUndefined()
                && unit.getName() != null
                && !unit.getName().isEmpty()
               );
    }
}
