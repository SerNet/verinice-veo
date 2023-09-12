/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.rest.common

import static java.util.UUID.randomUUID

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.adapter.presenter.api.dto.full.FullControlDto
import org.veo.adapter.presenter.api.dto.full.FullDomainDto
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto
import org.veo.adapter.presenter.api.dto.full.FullScopeDto
import org.veo.adapter.presenter.api.dto.full.LegacyCatalogItemDto
import org.veo.core.entity.Asset
import org.veo.core.entity.AssetRisk
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Element
import org.veo.core.entity.EntityType
import org.veo.core.entity.Incident
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.RiskAffected
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.compliance.ControlImplementation
import org.veo.rest.common.marshalling.ReferenceAssemblerImpl
import org.veo.rest.configuration.TypeExtractor

import spock.lang.Specification

class ReferenceAssemblerImplSpec extends Specification {

    TypeExtractor typeExtractor = Mock(TypeExtractor)

    ReferenceAssembler referenceAssembler = new ReferenceAssemblerImpl(typeExtractor)

    def "parsed entity id for #url is #parsedId"() {
        expect:
        referenceAssembler.parseId(url) == parsedId

        where:
        url                                                                                                              | parsedId
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454'                                              | '40331ed5-be07-4c69-bf99-553811ce5454'
        'http://localhost:9000/assets/e24e5c51-eb30-4150-8009-06ead941b321?foo=bar'                                      | 'e24e5c51-eb30-4150-8009-06ead941b321'
        // TODO: VEO-585: probably expect an exception instead
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454/risks/59d3c21d-2f21-4085-950d-1273056d664a'   | '59d3c21d-2f21-4085-950d-1273056d664a'
        'http://localhost:9000/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'                                            | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'
        'http://localhost:9000/scopes/59d3c21d-2f21-4085-950d-1273056d664a'                                              | '59d3c21d-2f21-4085-950d-1273056d664a'
        'http://localhost:9000/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509'                                           | 'f05ab334-c605-456e-8a78-9e1bc85b8509'
        'http://localhost:9000/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'                                           | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'
        'http://localhost:9000/domains/28df429d-da5e-431a-a2d8-488c0741fb9f'                                             | '28df429d-da5e-431a-a2d8-488c0741fb9f'
        'http://localhost:9000/catalogs/37dccbdc-7d58-4929-9d96-df8c533ea5a5/items/47799d6d-7887-48d5-9cd2-1af23e0b467a' | '47799d6d-7887-48d5-9cd2-1af23e0b467a'
        'http://veo-4c053c73-5242-4c79-9222-09609911b1f5:8070/veo/catalogs/ec2f0c9b-4a2b-429d-95af-83f107f07946/items/7688ddc3-6914-4899-a96a-067cc74cded1' | '7688ddc3-6914-4899-a96a-067cc74cded1'
    }

    def "parsed type for #url is #type"() {
        1 *  typeExtractor.parseDtoType(url) >> Optional.of(dtoType)

        expect:
        referenceAssembler.parseType(url) == type

        where:
        url                                                                                                              | type        | dtoType
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454'                                              | Asset       | FullAssetDto
        'http://localhost:9000/assets/8cdff5d7-3e17-4b80-80f6-e20a2cd4e673?foo=bar'                                      | Asset       | FullAssetDto
        // TODO: VEO-585: probably expect an exception instead
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454/risks/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'   | Asset       | FullAssetDto
        'http://localhost:9000/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'                                            | Control     | FullControlDto
        'http://localhost:9000/scopes/59d3c21d-2f21-4085-950d-1273056d664a'                                              | Scope       | FullScopeDto
        'http://localhost:9000/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509'                                           | Scenario    | FullScenarioDto
        'http://localhost:9000/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'                                           | Incident    | FullIncidentDto
        'http://localhost:9000/domains/28df429d-da5e-431a-a2d8-488c0741fb9f'                                             | Domain      | FullDomainDto
    }

    def "path traversal attack is tried for #url"() {
        1 *  typeExtractor.parseDtoType(url) >> Optional.empty()

        when: 'the url is parsed with the reference assembler'
        referenceAssembler.parseType(url)

        then: 'exception is thrown'
        IllegalArgumentException e = thrown()
        e.message =~ /Could not extract entity type from URI/

        where:
        url << [
            'http://localhost:9000/assets/00000000-0000-0000-0000-000000000000/%252e%252e/%252e%252e/processes/28df429d-da5e-431a-a2d8-488c0741fb9f'
        ]
    }

    def "parsed type for #url is #type with typeExtractor"() {
        1 *  typeExtractor.parseDtoType(url) >> Optional.of(dtoType)

        expect:
        referenceAssembler.parseType(url) == type

        where:
        url                                                                                                              | type        | dtoType
        // TODO: VEO-585: probably expect an exception instead
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454/risks/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'   | Asset       | FullAssetDto
        'http://localhost:9000/catalogs/37dccbdc-7d58-4929-9d96-df8c533ea5a5/items/47799d6d-7887-48d5-9cd2-1af23e0b467a' | CatalogItem | LegacyCatalogItemDto
    }

    def "target reference for #type and #id is #reference"() {
        given:
        def entity = Stub(type) {
            getId () >> Key.uuidFrom(id)
            getModelInterface() >> type
        }

        expect:
        referenceAssembler.targetReferenceOf(entity) == reference

        where:
        type     | id                                     | reference
        Asset    | '40331ed5-be07-4c69-bf99-553811ce5454' | '/assets/40331ed5-be07-4c69-bf99-553811ce5454'
        Control  | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | '/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'
        Scenario | 'f05ab334-c605-456e-8a78-9e1bc85b8509' | '/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509'
        Incident | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac' | '/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'
        Scope    | '59d3c21d-2f21-4085-950d-1273056d664a' | '/scopes/59d3c21d-2f21-4085-950d-1273056d664a'
        Domain   | '28df429d-da5e-431a-a2d8-488c0741fb9f' | '/domains/28df429d-da5e-431a-a2d8-488c0741fb9f'
    }

    def "create #entityType.singularTerm references in domain"() {
        given:
        def clazz = entityType.type as Class<Element>
        def elementId = randomUUID().toString()
        def domainId = randomUUID().toString()
        def element = Stub(clazz) {
            idAsString >> elementId
            modelInterface >> clazz
        }
        def domain = Stub(Domain) {
            idAsString >> domainId
        }

        expect:
        referenceAssembler.elementInDomainRefOf(element, domain) == "/domains/$domainId/${entityType.pluralTerm}/$elementId"

        where:
        entityType << EntityType.ELEMENT_TYPES
    }

    def "create target URI for catalog item"() {
        given:
        def catalogId = '371c5f43-cd7c-4e4f-b45b-59a7337bf489'
        def itemId = 'ccf66944-e782-4221-8e2a-65209d2826f1'
        Domain domain = Stub {
            getId () >> Key.uuidFrom(catalogId)
        }
        CatalogItem catalogItem = Stub {
            getId () >> Key.uuidFrom(itemId)
            getOwner()  >> domain
            getModelInterface() >> CatalogItem
        }

        expect:
        referenceAssembler.targetReferenceOf(catalogItem) == "/catalogs/${catalogId}/items/${itemId}"
    }

    def "resources reference for #type is #reference"() {
        expect:
        referenceAssembler.resourcesReferenceOf(type) == reference

        where:
        type           | reference
        Asset          | '/assets{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,updatedBy,size,page,sortBy,sortOrder,embedRisks}'
        Control        | '/controls{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,updatedBy,size,page,sortBy,sortOrder}'
        Scenario       | '/scenarios{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,updatedBy,size,page,sortBy,sortOrder}'
        Incident       | '/incidents{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,updatedBy,size,page,sortBy,sortOrder}'
        Scope          | '/scopes{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,updatedBy,size,page,sortBy,sortOrder,embedRisks}'
        Domain         | '/domains'
        DomainTemplate | '/domaintemplates'
    }

    def "searches reference for #type is #reference"() {
        expect:
        referenceAssembler.searchesReferenceOf(type) == reference

        where:
        type     | reference
        Asset    | '/assets/searches'
        Control  | '/controls/searches'
        Scenario | '/scenarios/searches'
        Incident | '/incidents/searches'
        Scope    | '/scopes/searches'
        Domain   | '/domains/searches'
    }

    def "target reference for #type and compound-id #entityId/#scenarioId is #reference"() {
        def risk = Stub(type) {
            entity >> Stub(entityType as Class<Element>) {
                id >> Key.uuidFrom(entityId)
            }
            scenario >> Stub(Scenario) {
                id >> Key.uuidFrom(scenarioId)
            }
        }

        when:
        def result = referenceAssembler.targetReferenceOf(risk)

        then:
        result == reference

        where:
        type        | entityType | entityId                               | scenarioId                             | reference
        AssetRisk   | Asset      | '40331ed5-be07-4c69-bf99-553811ce5454' | '5743c89a-5b17-4b50-8c21-72f2ac86faf3' | '/assets/40331ed5-be07-4c69-bf99-553811ce5454/risks/5743c89a-5b17-4b50-8c21-72f2ac86faf3'
        ProcessRisk | Process    | '40331ed5-be07-4c69-bf99-553811ce5454' | '5743c89a-5b17-4b50-8c21-72f2ac86faf3' | '/processes/40331ed5-be07-4c69-bf99-553811ce5454/risks/5743c89a-5b17-4b50-8c21-72f2ac86faf3'
    }

    def "requirement implementations reference for #type is generated"() {
        given:
        def controlImplementation = Spy(ControlImplementation) {
            owner >> Spy(type as Class<RiskAffected>) {
                id >> Key.uuidFrom("aff15bfa-7259-4044-a396-59db2e16b0e0")
            }
            control >> Spy(Control) {
                id >> Key.uuidFrom("da8f6256-15e0-4fd3-a11b-4a76c916abe5")
            }
        }

        when:
        def result = referenceAssembler.requirementImplementationsOf(controlImplementation)

        then:
        result == reference

        where:
        type    | reference
        Asset   | '/assets/aff15bfa-7259-4044-a396-59db2e16b0e0/control-implementations/da8f6256-15e0-4fd3-a11b-4a76c916abe5/requirement-implementations'
        Process | '/processes/aff15bfa-7259-4044-a396-59db2e16b0e0/control-implementations/da8f6256-15e0-4fd3-a11b-4a76c916abe5/requirement-implementations'
        Scope   | '/scopes/aff15bfa-7259-4044-a396-59db2e16b0e0/control-implementations/da8f6256-15e0-4fd3-a11b-4a76c916abe5/requirement-implementations'
    }

    def "create a key for a reference to a #type with id #id "() {
        expect:
        referenceAssembler.toKey(new IdRef(null, id, type, referenceAssembler)) == key

        where:
        type     | id                                     | key
        Asset    | '40331ed5-be07-4c69-bf99-553811ce5454' | Key.uuidFrom('40331ed5-be07-4c69-bf99-553811ce5454')
        Control  | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | Key.uuidFrom('c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6')
        Scenario | 'f05ab334-c605-456e-8a78-9e1bc85b8509' | Key.uuidFrom('f05ab334-c605-456e-8a78-9e1bc85b8509')
        Incident | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac' | Key.uuidFrom('7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac')
        Scope    | '59d3c21d-2f21-4085-950d-1273056d664a' | Key.uuidFrom('59d3c21d-2f21-4085-950d-1273056d664a')
        Domain   | '28df429d-da5e-431a-a2d8-488c0741fb9f' | Key.uuidFrom('28df429d-da5e-431a-a2d8-488c0741fb9f')
    }

    def "create multiple keys for a reference to a #type with keys #id1, #id2 "() {
        expect:
        def refs = referenceAssembler.toKeys([
            new IdRef(null, id1, type, referenceAssembler),
            new IdRef(null, id2, type, referenceAssembler)
        ] as Set
        )
        refs.contains(Key.uuidFrom(id1))
        refs.contains(Key.uuidFrom(id2))

        where:
        type     | id1                                    | id2
        Asset    | '40331ed5-be07-4c69-bf99-553811ce5454' | '5743c89a-5b17-4b50-8c21-72f2ac86faf3'
        Control  | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | '5743c89a-5b17-4b50-8c21-72f2ac86faf3'
        Scenario | 'f05ab334-c605-456e-8a78-9e1bc85b8509' | '9079c8bd-a6d9-4f72-b22c-ae75716869bc'
        Incident | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac' | '63a372c9-e34d-4c40-aa83-ee9aa43c8e8c'
        Scope    | '59d3c21d-2f21-4085-950d-1273056d664a' | '5c70c0b8-5882-4eaf-8bf8-98f9f5a923ea'
        Domain   | '28df429d-da5e-431a-a2d8-488c0741fb9f' | '28df429d-da5e-431a-a2d8-488c0741fb9f'
    }

    def "create an empty key reference"() {
        expect:
        referenceAssembler.toKey(null) == null
    }
}
