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
package org.veo.core.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;

public interface GenericElementRepository extends ElementQueryProvider<Element> {

  Collection<Element> saveAll(Collection<Element> entities);

  void deleteAll(Collection<Element> entities);

  Set<SubTypeStatusCount> getCountsBySubType(Unit u, Domain domain);

  @Deprecated
  // TODO: will be removed with #3950
  default <T extends Element> T getById(UUID elementId, Class<T> elementType, Client client) {
    return getById(elementId, elementType, client.getId());
  }

  default <T extends Element> T getById(
      UUID elementId, Class<T> elementType, UserAccessRights user) {
    return (T)
        findById(elementId, elementType, user)
            .orElseThrow(() -> new NotFoundException(elementId, elementType));
  }

  default <T extends Element> T getById(UUID elementId, Class<T> elementType, UUID clientId) {
    return findById(elementId, elementType, clientId)
        .orElseThrow(() -> new NotFoundException(elementId, elementType));
  }

  <T extends Element> Optional<T> findById(UUID elementId, Class<T> elementType, UUID clientId);

  <T extends Element> Optional<T> findById(
      UUID elementId, Class<? extends Element> elementType, UserAccessRights userRights);

  LinkQuery queryLinks(Element element, Domain domain);

  ParentElementQuery queryParents(Element element, Domain domain);

  /**
   * Delete all elements in the given unit. <b>This will clear the persistence context</b>, so all
   * entity references will become stale.
   */
  void deleteByUnit(Unit unit);
}
