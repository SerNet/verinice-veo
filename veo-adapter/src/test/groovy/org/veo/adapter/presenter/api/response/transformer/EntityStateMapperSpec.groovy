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

import java.time.LocalDate

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.dto.CustomLinkDto
import org.veo.adapter.presenter.api.dto.DomainAssociationDto
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto
import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto
import org.veo.adapter.presenter.api.dto.full.FullProcessDto
import org.veo.core.entity.Asset
import org.veo.core.entity.Control
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.compliance.Origination
import org.veo.core.entity.compliance.RequirementImplementation
import org.veo.core.entity.ref.ITypedId
import org.veo.core.entity.ref.TypedId
import org.veo.core.entity.state.PotentialImpactDomainAssociationState
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.service.EntityStateMapper
import org.veo.core.usecase.service.IdRefResolver

import spock.lang.Specification

class EntityStateMapperSpec extends Specification {

    def domain0Id = UUID.randomUUID()
    def domain1Id = UUID.randomUUID()
    Domain domain0 = Mock(Domain) { it.id >> domain0Id }
    Domain domain1 = Mock(Domain) { it.id >> domain1Id }
    IdRefResolver idRefResolver = Mock() {
        resolve(domain0Id, Domain) >> domain0
        resolve(TypedId.from(domain0Id, Domain)) >> domain0
        resolve(domain1Id, Domain) >> domain1
        resolve(TypedId.from(domain1Id, Domain)) >> domain1
    }
    EntityFactory entityFactory = Mock()
    EventPublisher eventPublisher = Mock()
    EntityStateMapper entityStateMapper = new EntityStateMapper(entityFactory, eventPublisher)

    def "maps domains from DTO to entity"() {
        given: "a process with two domains and a DTO with different sub types for those domains"
        FullProcessDto dto = Mock()
        Process entity = Mock()
        entity.modelInterface >> Process

        dto.controlImplementationStates >> []
        dto.requirementImplementationStates >> []
        dto.getDomainAssociationStates() >> [
            Mock(PotentialImpactDomainAssociationState) {
                subType >> "foo"
                status >> "NEW_FOO"
                riskValues >> [:]
                domain >> TypedId.from(domain0Id, Domain)
                customLinkStates >> []
                customAspectStates >> []
            },
            Mock(PotentialImpactDomainAssociationState) {
                subType >> "bar"
                status >> "NEW_BAR"
                riskValues >> [:]
                domain >> TypedId.from(domain1Id, Domain)
                customLinkStates >> []
                customAspectStates >> []
            }
        ]

        when: "the sub types are mapped"
        entityStateMapper.mapState(dto, entity, true, idRefResolver)

        then: "it is set"
        1 * entity.findSubType(domain0) >> Optional.empty()
        1 * entity.findSubType(domain1) >> Optional.empty()
        1 * entity.associateWithDomain(domain0, "foo", "NEW_FOO")
        1 * entity.associateWithDomain(domain1, "bar", "NEW_BAR")
        1 * entity.getLinks(domain0) >> []
        1 * entity.getLinks(domain1) >> []
        1 * entity.getCustomAspects(domain0) >> []
        1 * entity.getCustomAspects(domain1) >> []
        1 * entity.getControlImplementations() >> []
        1 * entity.getRequirementImplementations() >> []
        1 * entity.getDomains() >> []
    }

    def "Transform composite element DTO with parts to entity"() {
        given: "an asset composite element DTO with two parts"
        def asset1Ref = Mock(ITypedId)
        def asset2Ref = Mock(ITypedId)

        def asset1 = Mock(Asset)
        def asset2 = Mock(Asset)
        def newCompositeAssetEntity = Mock(Asset) {
            it.modelType >> Asset.SINGULAR_TERM
            it.controlImplementations >> []
            it.requirementImplementations >> []
        }

        def compositeAssetId = UUID.randomUUID()
        def compositeAssetDto = new FullAssetDto().tap {
            id = compositeAssetId
            name = "Composite Asset"
            setParts([
                asset1Ref,
                asset2Ref
            ] as Set)
        }

        when: "transforming the DTO to an entity"
        entityStateMapper.mapState(compositeAssetDto, newCompositeAssetEntity, true, idRefResolver)

        then: "the composite element is transformed with parts"
        1 * idRefResolver.resolve(Set.of(asset1Ref, asset2Ref)) >> [asset1, asset2]
        1 * newCompositeAssetEntity.setParts([asset1, asset2].toSet())
        1 * newCompositeAssetEntity.getDomains() >> []
    }

    def "Transform element DTO with links to entity"() {
        given: "an asset composite element DTO with two parts"
        def asset1Ref = Mock(IdRef)
        def domain0Ref = Stub(IdRef) {
            getId() >> domain0.id
        }

        def asset1 = Mock(Asset)
        def entity = Mock(Document)
        def link = Mock(CustomLink)

        def compositeAssetId = UUID.randomUUID()
        def dto = new FullDocumentDto().tap {
            id = compositeAssetId
            domains = [(domain0.id): new DomainAssociationDto().tap {
                    subType = "contract"
                    status = "CONCLUDED"
                } ]
            setLinks('hosting_server':[
                new CustomLinkDto().tap {
                    target = asset1Ref
                    attributes = ['path': '/srv/contract.txt']
                    domains = [domain0Ref]
                }
            ])
        }

        when: "transforming the DTO to an entity"
        entityStateMapper.mapState(dto, entity, true, idRefResolver)

        then: "the composite element is transformed with parts"
        1 * entity.findSubType(domain0) >> Optional.empty()
        1 * idRefResolver.resolve(asset1Ref) >> asset1
        1 * entity.getLinks(domain0) >> []
        1 * entity.getCustomAspects(domain0) >> []
        1 * entity.getDomains() >> []
        1 * entityFactory.createCustomLink(asset1, entity, 'hosting_server', domain0) >> link
        1 * link.setAttributes(['path': '/srv/contract.txt'])
        1 * entity.applyLink(link)
    }

    def "Transform asset DTO with RIs to entity"() {
        given:
        def entity = Mock(Asset) {
            it.modelType >> Asset.SINGULAR_TERM
            it.controlImplementations >> []
            it.requirementImplementations >> []
        }
        Control control = Mock()
        Person person = Mock()
        Document document = Mock()
        IdRef controlRef = Mock()
        IdRef personRef = Mock()
        IdRef documentRef = Mock()
        RequirementImplementation ri = Mock()

        def assetId = UUID.randomUUID()
        def dto = new FullAssetDto().tap {
            id = assetId
            name = "My asset"
            requirementImplementations = [
                new RequirementImplementationDto().tap {
                    it.control = controlRef
                    responsible = personRef
                    status = ImplementationStatus.PARTIAL
                    implementationUntil = '2025-01-01'
                    origination = Origination.SYSTEM_SPECIFIC
                    cost = 1
                    lastRevisionBy = personRef
                    it.document = documentRef
                    nextRevisionDate = '3000-01-01'
                    assessmentBy = personRef
                }
            ]
        }

        when:
        entityStateMapper.mapState(dto, entity, true, idRefResolver)

        then:
        1 * idRefResolver.resolve(controlRef) >> control
        3 * idRefResolver.resolve(personRef) >> person
        1 * idRefResolver.resolve(documentRef) >> document
        1 * entity.getDomains() >> []
        1 * entity.findRequirementImplementation(control) >> Optional.empty()
        1 * entity.addRequirementImplementation(control) >> ri
        1 * ri.setResponsible(person)
        1 * ri.setStatus(ImplementationStatus.PARTIAL)
        1 * ri.setImplementationUntil(LocalDate.parse('2025-01-01'))
        1 * ri.setOrigination(Origination.SYSTEM_SPECIFIC)
        1 * ri.setCost(1)
        1 * ri.setDocument(document)
        1 * ri.setLastRevisionBy(person)
        1 * ri.setNextRevisionDate(LocalDate.of(3000, 1, 1))
        1 * ri.setAssessmentBy(person)
    }
}