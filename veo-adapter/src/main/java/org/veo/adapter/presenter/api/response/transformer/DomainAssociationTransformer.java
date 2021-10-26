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
package org.veo.adapter.presenter.api.response.transformer;

import java.util.stream.Collectors;

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.service.domaintemplate.SyntheticIdRef;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

/**
 * Maps {@link Domain} associations of {@link Element}s between entities and
 * DTOs. See {@link DomainAssociationDto}.
 */
public class DomainAssociationTransformer {
    public void mapDomainsToEntity(AbstractElementDto source, Element target,
            IdRefResolver idRefResolver) {
        source.getDomains()
              .forEach((domainId, associationDto) -> {
                  var domain = idRefResolver.resolve(SyntheticIdRef.from(domainId, Domain.class));
                  target.addToDomains(domain);
                  domain.validateSubType(target.getModelInterface(), associationDto.getSubType());
                  target.setSubType(domain, associationDto.getSubType(),
                                    associationDto.getStatus());
              });
    }

    public void mapDomainsToDto(Element source, AbstractElementDto target) {
        target.setDomains(source.getDomains()
                                .stream()
                                .collect(Collectors.toMap(domain -> domain.getId()
                                                                          .uuidValue(),
                                                          domain -> {
                                                              var association = new DomainAssociationDto();
                                                              association.setSubType(source.getSubType(domain)
                                                                                           .orElse(null));
                                                              association.setStatus(source.getStatus(domain)
                                                                                          .orElse(null));
                                                              return association;
                                                          })));
    }
}
