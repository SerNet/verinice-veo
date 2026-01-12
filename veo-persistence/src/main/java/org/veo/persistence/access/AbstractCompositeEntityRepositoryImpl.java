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

import java.util.UUID;

import org.veo.core.entity.CompositeElement;
import org.veo.persistence.access.jpa.CompositeEntityDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.ValidationService;

abstract class AbstractCompositeEntityRepositoryImpl<
        S extends CompositeElement<?>, T extends ElementData & CompositeElement<?>>
    extends AbstractElementRepository<S, T> {

  private final CompositeEntityDataRepository<T> compositeRepo;

  AbstractCompositeEntityRepositoryImpl(
      CompositeEntityDataRepository<T> dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      ElementQueryFactory elementQueryFactory,
      Class<S> elementType) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        elementType);
    this.compositeRepo = dataRepository;
  }

  @Override
  public void deleteById(UUID id) {
    // remove element from composite parts:
    var composites = compositeRepo.findAllByParts(singleton(id));
    composites.forEach(assetComposite -> assetComposite.removePartById(id));

    super.deleteById(id);
  }
}
