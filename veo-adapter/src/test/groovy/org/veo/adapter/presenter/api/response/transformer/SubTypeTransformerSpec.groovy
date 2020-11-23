/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.response.transformer


import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto
import org.veo.core.entity.Domain
import org.veo.core.entity.EntityLayerSupertype
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.specification.DomainBoundaryViolationException

import spock.lang.Specification

class SubTypeTransformerSpec extends Specification {

    EntityLayerSupertypeDto dto = Mock()
    EntityLayerSupertype entity = Mock()
    SubTypeTransformer aspectTransformer = new SubTypeTransformer()
    Domain domain0 = Mock(Domain) { it.id >> Key.newUuid() }
    Domain domain1 = Mock(Domain) { it.id >> Key.newUuid() }

    def "maps sub types from DTO to entity"() {
        given: "an entity with two domains and a DTO with different sub types for those domains"
        entity.domains >> [domain0, domain1]
        entity.modelInterface >> Process
        dto.subType >> [
            (domain0.id.uuidValue()): "foo",
            (domain1.id.uuidValue()): "bar",
        ]

        when: "the sub types are mapped"
        aspectTransformer.mapSubTypesToEntity(dto, entity)
        then: "it is set"
        1 * entity.setSubType(domain0, "foo")
        1 * entity.setSubType(domain1, "bar")
        1 * domain0.validateSubType(Process, "foo")
        1 * domain1.validateSubType(Process, "bar")
    }

    def "sub type in invalid domain on DTO causes exception"() {
        given: "a DTO that defines a sub type for a domain that the entity does not belong to"
        entity.domains >> [domain0]
        dto.subType >> [
            (domain0.id.uuidValue()): "foo",
            (domain1.id.uuidValue()): "bar",
        ]

        when: "the sub types are mapped"
        aspectTransformer.mapSubTypesToEntity(dto, entity)
        then: "an exception is thrown"
        thrown(DomainBoundaryViolationException)
    }

    def "maps sub types from entity to DTO"() {
        given: "an entity with different sub types in two domains"
        entity.domains >> [domain0, domain1]
        entity.getSubType(domain0) >> Optional.of("foo")
        entity.getSubType(domain1) >> Optional.of("bar")

        when: "the sub types are mapped"
        aspectTransformer.mapSubTypesToDto(entity, dto)
        then: "a sub type map is set on the DTO"
        1 * dto.setSubType([
            (domain0.id.uuidValue()): "foo",
            (domain1.id.uuidValue()): "bar"
        ])
    }

    def "missing sub type on entity is not mapped"() {
        given: "an entity with one domain but no sub type for that domain"
        entity.domains >> [domain0]
        entity.getSubType(domain0) >> Optional.empty()

        when: "the sub types are mapped"
        aspectTransformer.mapSubTypesToDto(entity, dto)
        then: "the missing sub type is not added to the map"
        1 * dto.setSubType([:])
    }
}
