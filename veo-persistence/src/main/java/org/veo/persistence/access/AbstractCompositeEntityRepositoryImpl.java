/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import static java.util.Collections.singleton;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.repository.CompositeElementRepository;
import org.veo.persistence.access.jpa.CompositeEntityDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.ValidationService;

abstract class AbstractCompositeEntityRepositoryImpl<
        S extends CompositeElement<?>, T extends ElementData & CompositeElement<?>>
    extends AbstractElementRepository<S, T> implements CompositeElementRepository<S> {

  private final CompositeEntityDataRepository<T> compositeRepo;

  AbstractCompositeEntityRepositoryImpl(
      CompositeEntityDataRepository<T> dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository) {
    super(dataRepository, validation, linkDataRepository, scopeDataRepository);
    this.compositeRepo = dataRepository;
  }

  @Override
  public void deleteById(Key<UUID> id) {
    // remove element from composite parts:
    var composites = compositeRepo.findDistinctByParts_DbId_In(singleton(id.uuidValue()));
    composites.forEach(assetComposite -> assetComposite.removePartById(id));

    super.deleteById(id);
  }

  @Override
  public Set<S> findCompositesByParts(Set<S> parts) {
    var partIds = parts.stream().map(Identifiable::getIdAsString).collect(Collectors.toSet());
    return compositeRepo.findDistinctByParts_DbId_In(partIds).stream()
        .map(data -> (S) data)
        .collect(Collectors.toSet());
  }

  @Override
  @Transactional
  public Set<S> findByUnit(Unit owner) {
    var elements = dataRepository.findByUnits(singleton(owner.getId().uuidValue()));
    dataRepository.findWithScopesByUnits(singleton(owner.getId().uuidValue()));
    compositeRepo.findWithPartsByUnits(singleton(owner.getId().uuidValue()));
    return elements.stream().map(el -> (S) el).collect(Collectors.toSet());
  }
}
