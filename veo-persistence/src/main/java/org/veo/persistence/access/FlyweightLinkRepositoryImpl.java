/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.persistence.access;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.FlyweightElement;
import org.veo.core.repository.FlyweightLinkRepository;
import org.veo.persistence.access.jpa.FlyweightLinkDataRepostory;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class FlyweightLinkRepositoryImpl implements FlyweightLinkRepository {
  private final FlyweightLinkDataRepostory linkRepo;

  @Override
  public Set<FlyweightElement> findAllLinksGroupedByElement(
      Set<String> types, UUID domainId, UUID unitId, UUID clientId) {
    return linkRepo.findAllLinksGroupedByElement(types, domainId, unitId, clientId).stream()
        .map(FlyweightElement.class::cast)
        .collect(Collectors.toSet());
  }
}
