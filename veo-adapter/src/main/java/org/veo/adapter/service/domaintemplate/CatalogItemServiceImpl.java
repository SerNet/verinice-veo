/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate;

import java.util.Collections;

import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.service.CatalogItemService;
import org.veo.core.usecase.service.EntityStateMapper;

public class CatalogItemServiceImpl implements CatalogItemService {
  private final DtoToEntityTransformer entityTransformer;
  private final EntityToDtoTransformer dtoTransformer;
  private final CatalogItemPrepareStrategy preparations;

  public CatalogItemServiceImpl(
      EntityToDtoTransformer dtoTransformer,
      EntityFactory factory,
      IdentifiableFactory identifiableFactory,
      DomainAssociationTransformer domainAssociationTransformer,
      EntityStateMapper entityStateMapper,
      CatalogItemPrepareStrategy preparations) {
    this.dtoTransformer = dtoTransformer;
    this.preparations = preparations;

    this.entityTransformer =
        new DtoToEntityTransformer(
            factory, identifiableFactory, domainAssociationTransformer, entityStateMapper);
  }

  @Override
  public Element createInstance(CatalogItem item, Domain domain) {
    Element catalogElement = item.getElement();
    String idAsString = item.getCatalog().getDomainTemplate().getIdAsString();
    AbstractElementDto dto = dtoTransformer.transform2Dto(catalogElement);
    prepareDto(dto);
    PlaceholderResolver placeholderResolver = new PlaceholderResolver(entityTransformer);
    placeholderResolver.cache.put(idAsString, domain);
    Element newElement = entityTransformer.transformDto2Element(dto, placeholderResolver);
    preparations.prepareElement(domain, newElement, false);
    if (newElement instanceof Process pn) {
      Process po = (Process) catalogElement;
      po.getImpactValues(item.getCatalog().getDomainTemplate())
          .ifPresent(impactValues -> pn.setImpactValues(domain, impactValues));
    } else if (newElement instanceof Scenario sn) {
      Scenario so = (Scenario) catalogElement;
      so.getPotentialProbability(item.getCatalog().getDomainTemplate())
          .ifPresent(
              potentialProbability -> sn.setPotentialProbability(domain, potentialProbability));
    } else if (newElement instanceof Control cn) {
      Control co = (Control) catalogElement;
      co.getRiskValues(item.getCatalog().getDomainTemplate())
          .ifPresent(riskValues -> cn.setRiskValues(domain, riskValues));
    }
    // TODO: VEO-612 handle parts
    newElement.setAppliedCatalogItems(Collections.singleton(item));
    return newElement;
  }

  /** Prepare the dto for transformation by removing references, like links. */
  private void prepareDto(AbstractElementDto dto) {
    dto.setOwner(null);
    dto.getLinks().clear();
  }
}
