/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Alexander Koderman.
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
package org.veo.persistence.access.jpa;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.veo.core.entity.FlyweightElement;
import org.veo.core.entity.FlyweightLink;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.FlyweightElementData;

public interface FlyweightLinkDataRepostory extends JpaRepository<ElementData, String> {

  @Query(
      value =
          "SELECT source_id::text AS sourceId, target_id::text AS targetId, type "
              + "FROM customlink l, element e, unit u "
              + "WHERE type IN :types AND domain_id = :domainId "
              + "AND source_id = e.db_id "
              + "AND e.owner_id = u.db_id "
              + "AND e.owner_id = :unitId "
              + "AND u.client_id = :clientId ",
      nativeQuery = true)
  Set<FlyweightLink> findAllLinks(
      @Param("types") Set<String> types,
      @Param("domainId") UUID domainId,
      @Param("unitId") UUID unitId,
      @Param("clientId") UUID clientId);

  default Set<FlyweightElement> findAllLinksGroupedByElement(
      Set<String> types, UUID domainId, UUID unitId, UUID clientId) {
    Set<FlyweightLink> allFlyweightElements = findAllLinks(types, domainId, unitId, clientId);
    Map<String, FlyweightElement> elementsById =
        allFlyweightElements.stream()
            .collect(Collectors.groupingBy(FlyweightLink::sourceId))
            .entrySet()
            .stream()
            .map(entry -> new FlyweightElementData(entry.getKey(), new HashSet<>(entry.getValue())))
            .collect(Collectors.toMap(FlyweightElementData::sourceId, Function.identity()));

    Set<FlyweightElement> allLeafs =
        allFlyweightElements.stream()
            .collect(Collectors.groupingBy(FlyweightLink::targetId))
            .entrySet()
            .stream()
            .map(
                e ->
                    elementsById.getOrDefault(
                        e.getKey(), new FlyweightElementData(e.getKey(), new HashSet<>())))
            .collect(Collectors.toSet());

    Set<FlyweightElement> allNonLeafs =
        elementsById.entrySet().stream().map(Entry::getValue).collect(Collectors.toSet());
    Set<FlyweightElement> all = new HashSet<>(allLeafs);
    all.addAll(allNonLeafs);
    return all;
  }
}
