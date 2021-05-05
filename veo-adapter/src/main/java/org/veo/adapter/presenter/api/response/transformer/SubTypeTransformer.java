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

import java.util.HashMap;

import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.specification.DomainBoundaryViolationException;

/**
 * Maps sub type values between DTOs and entities.
 */
public class SubTypeTransformer {

    public void mapSubTypesToEntity(EntityLayerSupertypeDto source, EntityLayerSupertype target) {
        source.getSubType()
              .forEach((domainId, subTypeInDomain) -> {
                  var domain = target.getDomains()
                                     .stream()
                                     .filter((d) -> d.getId()
                                                     .uuidValue()
                                                     .equals(domainId))
                                     .findFirst()
                                     .orElseThrow(() -> new DomainBoundaryViolationException(
                                             String.format("Sub type can't be defined for domain %s because the entity %s does not belong to that domain.",
                                                           domainId, target.getId())));
                  domain.validateSubType(target.getModelInterface(), subTypeInDomain);
                  target.setSubType(domain, subTypeInDomain);
              });
    }

    public void mapSubTypesToDto(EntityLayerSupertype source, EntityLayerSupertypeDto target) {
        var subTypeMap = new HashMap<String, String>();
        for (var domain : source.getDomains()) {
            source.getSubType(domain)
                  .ifPresent((subType) -> subTypeMap.put(domain.getId()
                                                               .uuidValue(),
                                                         subType));
        }
        target.setSubType(subTypeMap);
    }
}
