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
package org.veo.adapter.presenter.api.response.transformer


import org.veo.adapter.IdRefResolver
import org.veo.adapter.presenter.api.dto.AbstractProcessDto
import org.veo.adapter.presenter.api.dto.DomainAssociationDto
import org.veo.adapter.service.domaintemplate.SyntheticIdRef
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.aspects.SubTypeAspect

import spock.lang.Specification

class DomainAssociationTransformerSpec extends Specification {

    Domain domain0 = Mock(Domain) { it.id >> Key.newUuid() }
    Domain domain1 = Mock(Domain) { it.id >> Key.newUuid() }
    IdRefResolver idRefResolver = Mock() {
        resolve(SyntheticIdRef.from(domain0.id.uuidValue(), Domain)) >> domain0
        resolve(SyntheticIdRef.from(domain1.id.uuidValue(), Domain)) >> domain1
    }
    DomainAssociationTransformer domainAssociationTransformer = new DomainAssociationTransformer()

    def "maps domains from DTO to entity"() {
        given: "a process with two domains and a DTO with different sub types for those domains"
        AbstractProcessDto dto = Mock()
        Process entity = Mock()
        entity.modelInterface >> Process
        dto.domains >> [
            (domain0.id.uuidValue()): Mock(DomainAssociationDto) {
                subType >> "foo"
                status >> "NEW_FOO"
            },
            (domain1.id.uuidValue()): Mock(DomainAssociationDto) {
                subType >> "bar"
                status >> "NEW_BAR"
            }
        ]

        when: "the sub types are mapped"
        domainAssociationTransformer.mapDomainsToEntity(dto, entity, idRefResolver)
        then: "it is set"
        1 * entity.addToDomains(domain0)
        1 * entity.addToDomains(domain1)
        1 * entity.setSubType(domain0, "foo", "NEW_FOO")
        1 * entity.setSubType(domain1, "bar", "NEW_BAR")
        1 * domain0.validateSubType(Process, "foo")
        1 * domain1.validateSubType(Process, "bar")
    }

    def "maps sub types from entity to DTO"() {
        given: "a process with different sub types in two domains"
        AbstractProcessDto dto = Mock()
        Process entity = Mock()
        Map<String, DomainAssociationDto> capturedDomainMap
        entity.domains >> [domain0, domain1]
        entity.subTypeAspects >> [
            Mock(SubTypeAspect) {
                domain >> domain0
                subType >> "foo"
                status >> "NEW_FOO"
            },
            Mock(SubTypeAspect) {
                domain >> domain1
                subType >> "bar"
                status >> "NEW_BAR"
            }
        ]
        entity.getSubType(domain0) >> Optional.of("foo")
        entity.getStatus(domain0) >> Optional.of("NEW_FOO")
        entity.getSubType(domain1) >> Optional.of("bar")
        entity.getStatus(domain1) >> Optional.of("NEW_BAR")

        when: "the sub types are mapped"
        domainAssociationTransformer.mapDomainsToDto(entity, dto)

        then: "a map of domain associations is set on the DTO"
        1 * dto.setDomains(_) >> { params -> capturedDomainMap = params[0]}
        capturedDomainMap.size() == 2
        with(capturedDomainMap[domain0.id.uuidValue()]) {
            subType == "foo"
            status == "NEW_FOO"
        }
        with(capturedDomainMap[domain1.id.uuidValue()]) {
            subType == "bar"
            status == "NEW_BAR"
        }
    }
}
