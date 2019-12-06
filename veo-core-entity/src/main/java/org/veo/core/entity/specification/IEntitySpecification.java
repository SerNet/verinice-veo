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
 ******************************************************************************/
package org.veo.core.entity.specification;

import java.util.Collection;
import java.util.Set;

import org.veo.core.entity.EntityLayerSupertype;

/**
 * An intention-revealing interface for business rule specifications.
 *
 * Specifications are used by entity builders and constructors to ensure that
 * all entities are built to spec.
 *
 * Specifications are also used by setter-methods and other state-changing
 * operations to ensure that the overall business model stays valid.
 *
 */
public interface IEntitySpecification<T extends EntityLayerSupertype> {

    public boolean isSatisfiedBy(T entity);

    public Set<T> selectSatisfyingElementsFrom(Collection<T> collection);
}
