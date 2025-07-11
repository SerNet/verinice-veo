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

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Unit;

/**
 * A dynamic database query for retrieving {@link Element} objects.
 *
 * @param <T> Entity type
 */
public interface ElementQuery<T extends Element> extends Query<T, String> {

  void whereUnitIn(Set<Unit> units);

  void whereSubTypeMatches(QueryCondition<String> values);

  void whereSubTypeMatches(QueryCondition<String> values, Domain domain);

  /**
   * Only include elements where at least one of its child elements (members or parts) has one of
   * the given IDs.
   *
   * @param elementIds elements IDs to be matched against the child element IDs. UUIDs are assumed
   *     to be unique across different types of elements.
   * @return this
   */
  void whereChildElementIn(QueryCondition<UUID> elementIds);

  /**
   * Only include elements with / without at least one child element (member or part).
   *
   * @param present pass true to only include elements with child elements, pass false to only
   *     include elements without child elements
   * @return this
   */
  void whereChildElementsPresent(boolean present);

  /**
   * Only include elements with / without at leat one parent element (scope or composite).
   *
   * @param present pass true only include elements with parent elements, pass false to only include
   *     elements without parent elements.
   * @return this
   */
  void whereParentElementPresent(boolean present);

  void whereStatusMatches(QueryCondition<String> values);

  void whereDisplayNameMatchesIgnoringCase(QueryCondition<String> values);

  void whereDescriptionMatchesIgnoreCase(QueryCondition<String> values);

  void whereDesignatorMatchesIgnoreCase(QueryCondition<String> values);

  void whereNameMatchesIgnoreCase(QueryCondition<String> values);

  void whereUpdatedByContainsIgnoreCase(QueryCondition<String> values);

  void whereAppliedItemIn(Collection<CatalogItem> items, Domain domain);

  void whereOwnerIs(Unit unit);

  void fetchAppliedCatalogItems();

  void fetchParentsAndChildrenAndSiblings();

  void fetchChildren();

  void fetchRisks();

  void fetchRiskValuesAspects();

  void whereDomainsContain(Domain domain);

  void whereScopesContain(SingleValueQueryCondition<UUID> scopeId);

  void fetchControlImplementations();

  void fetchRequirementImplementations();

  void whereAbbreviationMatchesIgnoreCase(QueryCondition<String> abbreviation);

  void whereIdIn(QueryCondition<UUID> ids);

  void whereElementTypeMatches(QueryCondition<ElementType> elementType);
}
