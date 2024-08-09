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

import static java.util.UUID.randomUUID

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.AbstractAssetDto
import org.veo.adapter.presenter.api.dto.AbstractProcessDto
import org.veo.adapter.presenter.api.dto.AssetDomainAssociationDto
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.aspects.SubTypeAspect
import org.veo.core.entity.decision.DecisionResult

import spock.lang.Specification

class DomainAssociationTransformerSpec extends Specification {

    Domain domain0 = Mock(Domain) { it.idAsUUID >> randomUUID() }
    Domain domain1 = Mock(Domain) { it.idAsUUID >> randomUUID() }
    ReferenceAssembler referenceAssembler = Mock()
    DomainAssociationTransformer domainAssociationTransformer = new DomainAssociationTransformer(referenceAssembler)

    def "maps sub types from entity to DTO"() {
        given: "a process with different sub types in two domains"
        Person linkTargetPerson = Mock(Person) {
            idAsUUID >> randomUUID()
            modelInterface >> Person
            findSubType(domain1) >> Optional.of("SuperOverseer")
        }
        referenceAssembler.targetReferenceOf(linkTargetPerson) >> "/persons/123"
        AbstractProcessDto dto = Mock()
        Process entity = Mock()
        entity.getImpactValues(_) >> [:]
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
        entity.getCustomAspects(domain0) >> [
            Mock(CustomAspect) {
                type >> "relevance"
                attributes >> [
                    importance: 10000
                ]
            }
        ]
        entity.getCustomAspects(domain1) >> []
        entity.getLinks(domain0) >> []
        entity.getLinks(domain1) >> [
            Mock(CustomLink) {
                type >> "overseer"
                target >> linkTargetPerson
                attributes >> [
                    qualified: true
                ]
            }
        ]
        def decisionResults0 = ["decision0": Mock(DecisionResult)]
        def decisionResults1 = ["decision1": Mock(DecisionResult)]

        entity.getSubType(domain0) >> "foo"
        entity.getStatus(domain0) >> "NEW_FOO"
        entity.getDecisionResults(domain0) >> decisionResults0
        entity.getSubType(domain1) >> "bar"
        entity.getStatus(domain1) >> "NEW_BAR"
        entity.getDecisionResults(domain1) >> decisionResults1

        when: "the sub types are mapped"
        domainAssociationTransformer.mapDomainsToDto(entity, dto, true)

        then: "a map of domain associations is set on the DTO"
        1 * dto.setDomains(_) >> { params -> capturedDomainMap = params[0]}
        capturedDomainMap.size() == 2
        with(capturedDomainMap[domain0.idAsUUID]) {
            subType == "foo"
            status == "NEW_FOO"
            customAspects.value.relevance.value.importance == 10000
            links.value.isEmpty()
            decisionResults == decisionResults0
        }
        with(capturedDomainMap[domain1.idAsUUID]) {
            subType == "bar"
            status == "NEW_BAR"
            customAspects.value.isEmpty()
            links.value.overseer[0].target.targetUri == "/persons/123"
            links.value.overseer[0].target.subType == "SuperOverseer"
            links.value.overseer[0].attributes.value.qualified
            decisionResults == decisionResults1
        }
    }

    def "maps sub types from entity to DTO asset"() {
        given: "an asset with different sub types in two domains"
        AbstractAssetDto dto = Mock()
        Asset entity = Mock()
        entity.getImpactValues(_) >> [:]
        Map<String, AssetDomainAssociationDto> capturedDomainMap
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

        entity.getSubType(domain0) >> "foo"
        entity.getStatus(domain0) >> "NEW_FOO"
        entity.getSubType(domain1) >> "bar"
        entity.getStatus(domain1) >> "NEW_BAR"
        entity.getCustomAspects(_) >> []
        entity.getLinks(_) >> []

        when: "the sub types are mapped"
        domainAssociationTransformer.mapDomainsToDto(entity, dto, true)

        then: "a map of domain associations is set on the DTO"
        1 * dto.setDomains(_) >> { params -> capturedDomainMap = params[0]}
        capturedDomainMap.size() == 2
        with(capturedDomainMap[domain0.idAsUUID]) {
            subType == "foo"
            status == "NEW_FOO"
        }
        with(capturedDomainMap[domain1.idAsUUID]) {
            subType == "bar"
            status == "NEW_BAR"
        }
    }
}
