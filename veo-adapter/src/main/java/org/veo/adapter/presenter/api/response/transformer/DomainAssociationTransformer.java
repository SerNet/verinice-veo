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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.service.domaintemplate.SyntheticIdRef;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.exception.ModelConsistencyException;

/**
 * Maps {@link Domain} associations of {@link Element}s between entities and
 * DTOs. See {@link DomainAssociationDto}.
 */
public class DomainAssociationTransformer {
    public void mapDomainsToEntity(AbstractElementDto source, Element target,
            IdRefResolver idRefResolver) {
        source.getDomains()
              .forEach((domainId, associationDto) -> {
                  Object resolvedObject = idRefResolver.resolve(SyntheticIdRef.from(domainId,
                                                                                    Domain.class));
                  if (hasInterface(resolvedObject, Domain.class)) {
                      var domain = (Domain) resolvedObject;
                      target.addToDomains(domain);
                      domain.validateSubType(target.getModelInterface(),
                                             associationDto.getSubType());
                      target.setSubType(domain, associationDto.getSubType(),
                                        associationDto.getStatus());
                  } else if (hasInterface(resolvedObject, DomainTemplate.class)) {
                      target.setSubType((DomainTemplate) resolvedObject,
                                        associationDto.getSubType(), associationDto.getStatus());
                  } else {
                      throw new ModelConsistencyException(
                              "Resolved object is not of any reconized type %s",
                              resolvedObject.toString());
                  }
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

    /**
     * Test if this actual is a domain object.(even if it's a Test Mock)
     */
    private boolean hasInterface(Object resolve, Class<? extends Identifiable> targetInterface) {
        Class<?>[] interfaces = resolve.getClass()
                                       .getInterfaces();
        return Arrays.stream(interfaces)
                     .filter(c -> {
                         return c.getCanonicalName()
                                 .equals(targetInterface.getCanonicalName());
                     })
                     .findAny()
                     .isPresent();
    }

    /**
     * Maps the aspects to a domaintemplate. We have some preconditions, the domains
     * must be empty as the element belongs to a domain template and therefore only
     * one aspect can be present.
     */
    public void mapDomainsToDto(Element source, AbstractElementDto target,
            DomainTemplate domainTemplate) {
        if (source.getDomains()
                  .isEmpty()
                && source.getSubTypeAspects()
                         .size() == 1) {
            SubTypeAspect subTypeAspect = source.getSubTypeAspects()
                                                .iterator()
                                                .next();
            DomainAssociationDto association = new DomainAssociationDto();
            association.setStatus(subTypeAspect.getStatus());
            association.setSubType(subTypeAspect.getSubType());

            target.setDomains(Map.of(domainTemplate.getIdAsString(), association));
        }
    }
}
