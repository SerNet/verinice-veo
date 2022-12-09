/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.adapter.presenter.api.io.mapper;

import org.veo.adapter.IdRefResolvingFactory;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.core.entity.Key;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;

/**
 * Maps between {@link TransformDomainTemplateDto} and {@link
 * CreateDomainTemplateUseCase.InputData}.
 */
public class CreateDomainTemplateInputMapper {
  public static CreateDomainTemplateUseCase.InputData map(
      TransformDomainTemplateDto domainTemplateDto,
      IdentifiableFactory identifiableFactory,
      EntityFactory entityFactory,
      DomainAssociationTransformer domainAssociationTransformer) {
    var resolvingFactory = new IdRefResolvingFactory(identifiableFactory);

    // Define an arbitrary temporary domain template ID and redirect any domain
    // references to our domain template (so we can import not only domain templates
    // but also domains).
    domainTemplateDto.setId(Key.newUuid().uuidValue());
    resolvingFactory.setGlobalDomainTemplateId(domainTemplateDto.getId());

    var transformer =
        new DtoToEntityTransformer(entityFactory, resolvingFactory, domainAssociationTransformer);

    var newDomainTemplate =
        transformer.transformTransformDomainTemplateDto2DomainTemplate(
            domainTemplateDto, resolvingFactory);

    return new CreateDomainTemplateUseCase.InputData(newDomainTemplate);
  }
}
