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

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.veo.core.entity.Element;

/**
 * An intention-revealing interface for business rule specifications.
 *
 * <p>Specifications are used by entity builders and constructors to ensure that all entities are
 * built to spec.
 *
 * <p>Specifications are also used by setter-methods and other state-changing operations to ensure
 * that the overall business model stays valid.
 *
 * <p>If a repositories is given a specification it can implement its own version that can based on
 * database-queries.
 */
public interface EntitySpecification<T> extends Predicate<T> {

  /**
   * Determines if this specification is fulfilled by the provided entity.
   *
   * @param entity The entity to test.
   * @return {@literal true} if the entity fulfills the specification. {@literal false} otherwise.
   */
  @Override
  boolean test(T entity);

  /**
   * @deprecated use {@link #test(Element)}
   */
  @Deprecated
  default boolean isSatisfiedBy(T entity) {
    return test(entity);
  }

  /**
   * Return only those entities from a collection that fulfill the {@code isSatisfiedBy()}
   * condition.
   *
   * @param collection a collection of elements to filter by the specification's condition
   * @return all entities fulfilling the specification
   */
  default Set<T> selectSatisfyingElementsFrom(Collection<T> collection) {
    return collection.stream().filter(this::test).collect(Collectors.toSet());
  }
}
