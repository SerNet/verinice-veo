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

import org.veo.adapter.presenter.api.dto.AbstractProcessDto
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.aspects.SubTypeAspect
import org.veo.core.entity.decision.DecisionResult
import org.veo.core.usecase.service.IdRefResolver
import org.veo.core.usecase.service.TypedId

import spock.lang.Specification

class DomainAssociationTransformerSpec extends Specification {

    Domain domain0 = Mock(Domain) { it.id >> Key.newUuid() }
    Domain domain1 = Mock(Domain) { it.id >> Key.newUuid() }
    IdRefResolver idRefResolver = Mock() {
        resolve(TypedId.from(domain0.id.uuidValue(), Domain)) >> domain0
        resolve(TypedId.from(domain1.id.uuidValue(), Domain)) >> domain1
    }
    DomainAssociationTransformer domainAssociationTransformer = new DomainAssociationTransformer()

    def "maps domains from DTO to entity"() {
        given: "a process with two domains and a DTO with different sub types for those domains"
        AbstractProcessDto dto = Mock()
        Process entity = Mock()
        entity.modelInterface >> Process
        dto.domains >> [
            (domain0.id.uuidValue()): Mock(ProcessDomainAssociationDto) {
                subType >> "foo"
                status >> "NEW_FOO"
                riskValues >> [:]
            },
            (domain1.id.uuidValue()): Mock(ProcessDomainAssociationDto) {
                subType >> "bar"
                status >> "NEW_BAR"
                riskValues >> [:]
            }
        ]

        when: "the sub types are mapped"
        domainAssociationTransformer.mapDomainsToEntity(dto, entity, idRefResolver)

        then: "it is set"
        1 * entity.associateWithDomain(domain0, "foo", "NEW_FOO")
        1 * entity.associateWithDomain(domain1, "bar", "NEW_BAR")
    }

    def "maps sub types from entity to DTO"() {
        given: "a process with different sub types in two domains"
        AbstractProcessDto dto = Mock()
        Process entity = Mock()
        entity.getImpactValues(_) >> Optional.empty()
        Map<String, ProcessDomainAssociationDto> capturedDomainMap
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
        def decisionResults0 = ["decision0": Mock(DecisionResult)]
        def decisionResults1 = ["decision1": Mock(DecisionResult)]

        entity.findSubType(domain0) >> Optional.of("foo")
        entity.findStatus(domain0) >> Optional.of("NEW_FOO")
        entity.getDecisionResults(domain0) >> decisionResults0
        entity.findSubType(domain1) >> Optional.of("bar")
        entity.findStatus(domain1) >> Optional.of("NEW_BAR")
        entity.getDecisionResults(domain1) >> decisionResults1

        when: "the sub types are mapped"
        domainAssociationTransformer.mapDomainsToDto(entity, dto)

        then: "a map of domain associations is set on the DTO"
        1 * dto.setDomains(_) >> { params -> capturedDomainMap = params[0]}
        capturedDomainMap.size() == 2
        with(capturedDomainMap[domain0.id.uuidValue()]) {
            subType == "foo"
            status == "NEW_FOO"
            decisionResults == decisionResults0
        }
        with(capturedDomainMap[domain1.id.uuidValue()]) {
            subType == "bar"
            status == "NEW_BAR"
            decisionResults == decisionResults1
        }
    }
}
