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
package org.veo.adapter.presenter.api

import org.veo.adapter.presenter.api.dto.AbstractElementDto
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto
import org.veo.adapter.presenter.api.dto.DomainAssociationDto
import org.veo.adapter.presenter.api.response.IdentifiableDto
import org.veo.core.entity.ElementType

import spock.lang.Specification

class ElementTypeDtoInfoSpec extends Specification {
    def "DTO info for #elementType.pluralTerm is correct"() {
        when:
        var dtoInfo = ElementTypeDtoInfo.get(elementType)

        then:
        AbstractElementDto.isAssignableFrom(dtoInfo.fullDtoClass)
        IdentifiableDto.isAssignableFrom(dtoInfo.fullDtoClass)
        AbstractElementInDomainDto.isAssignableFrom(dtoInfo.fullDomainSpecificDtoClass)
        IdentifiableDto.isAssignableFrom(dtoInfo.fullDomainSpecificDtoClass)
        DomainAssociationDto.isAssignableFrom(dtoInfo.domainAssociationDtoClass)

        when: "creating the DTOs"
        var fullDto = dtoInfo.fullDtoClass.newInstance(new Object[0])
        var fullDomainSpecificDto = dtoInfo.fullDomainSpecificDtoClass.newInstance(new Object[0])

        then: "they have the correct model class"
        fullDto.modelInterface.equals(elementType.type)
        fullDomainSpecificDto.modelInterface.equals(elementType.type)

        where:
        elementType << ElementType.values()
    }
}
