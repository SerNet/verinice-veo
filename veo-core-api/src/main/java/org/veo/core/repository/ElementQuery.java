/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.repository;

import java.util.Set;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;

/**
 * A dynamic database query for retrieving {@link Element} objects.
 *
 * @param <T>
 *            Entity type
 */
public interface ElementQuery<T extends Element> {

    ElementQuery<T> whereUnitIn(Set<Unit> units);

    ElementQuery<T> whereSubTypeMatches(QueryCondition<String> values);

    ElementQuery<T> whereStatusMatches(QueryCondition<String> values);

    ElementQuery<T> whereDisplayNameMatchesIgnoringCase(QueryCondition<String> values);

    ElementQuery<T> whereDescriptionMatchesIgnoreCase(QueryCondition<String> values);

    ElementQuery<T> whereDesignatorMatchesIgnoreCase(QueryCondition<String> values);

    ElementQuery<T> whereNameMatchesIgnoreCase(QueryCondition<String> values);

    ElementQuery<T> whereUpdatedByContainsIgnoreCase(QueryCondition<String> values);

    ElementQuery<T> whereAppliedItemsContains(CatalogItem item);

    ElementQuery<T> whereOwnerIs(Unit unit);

    PagedResult<T> execute(PagingConfiguration pagingConfiguration);
}
