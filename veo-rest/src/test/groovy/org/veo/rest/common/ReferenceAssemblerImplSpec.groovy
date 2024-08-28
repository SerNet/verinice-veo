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

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.entity.Asset
import org.veo.core.entity.AssetRisk
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainBase
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Element
import org.veo.core.entity.EntityType
import org.veo.core.entity.Identifiable
import org.veo.core.entity.Incident
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.Profile
import org.veo.core.entity.ProfileItem
import org.veo.core.entity.RiskAffected
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.ScopeRisk
import org.veo.core.entity.SymIdentifiable
import org.veo.core.entity.compliance.ControlImplementation
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.entity.ref.TypedId
import org.veo.core.entity.ref.TypedSymbolicId
import org.veo.rest.common.marshalling.ReferenceAssemblerImpl

import spock.lang.Specification

class ReferenceAssemblerImplSpec extends Specification {

    ReferenceAssembler referenceAssembler = new ReferenceAssemblerImpl()

    def "#type #id is extracted from #url"() {
        when:
        def ref = referenceAssembler.parseIdentifiableRef(url)

        then:
        ref.id.toString() == id
        ref.type == type

        and:
        referenceAssembler.parseIdentifiableRef('http://localhost:9000' + url) == ref
        referenceAssembler.parseIdentifiableRef('http://localhost:9000/apps/veo' + url) == ref

        where:
        url                                                    | type     | id
        '/assets/40331ed5-be07-4c69-bf99-553811ce5454'         | Asset    | '40331ed5-be07-4c69-bf99-553811ce5454'
        '/assets/e24e5c51-eb30-4150-8009-06ead941b321?foo=bar' | Asset    | 'e24e5c51-eb30-4150-8009-06ead941b321'
        '/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'       | Control  | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'
        '/scopes/59d3c21d-2f21-4085-950d-1273056d664a'         | Scope    | '59d3c21d-2f21-4085-950d-1273056d664a'
        '/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509'      | Scenario | 'f05ab334-c605-456e-8a78-9e1bc85b8509'
        '/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'      | Incident | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'
        '/domains/28df429d-da5e-431a-a2d8-488c0741fb9f'        | Domain   | '28df429d-da5e-431a-a2d8-488c0741fb9f'
    }

    def "target reference for #type and #id is #reference"() {
        given:
        def entity = Stub(type) {
            getIdAsUUID () >> UUID.fromString(id)
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
        def elementId = randomUUID()
        def domainId = randomUUID()
        def element = Stub(clazz) {
            idAsUUID >> elementId
            modelInterface >> clazz
        }
        def domain = Stub(Domain) {
            idAsUUID >> domainId
        }

        expect:
        referenceAssembler.elementInDomainRefOf(element, domain) == "/domains/$domainId/${entityType.pluralTerm}/$elementId"

        where:
        entityType << EntityType.ELEMENT_TYPES
    }

    def "create target URI for catalog item"() {
        given:
        def domainId = UUID.fromString('371c5f43-cd7c-4e4f-b45b-59a7337bf489')
        def itemId = 'ccf66944-e782-4221-8e2a-65209d2826f1'
        Domain domain = Stub {
            idAsUUID >> domainId
        }
        CatalogItem catalogItem = Stub {
            symbolicIdAsString >> itemId
            domainBase >> domain
            modelInterface >> CatalogItem
        }

        expect:
        referenceAssembler.targetReferenceOf(catalogItem) == "/domains/${domainId}/catalog-items/${itemId}"
    }

    def "resources reference for #type is #reference"() {
        expect:
        referenceAssembler.resourcesReferenceOf(type) == reference

        where:
        type           | reference
        Asset          | '/assets{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,abbreviation,updatedBy,size,page,sortBy,sortOrder,embedRisks}'
        Control        | '/controls{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,abbreviation,updatedBy,size,page,sortBy,sortOrder}'
        Scenario       | '/scenarios{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,abbreviation,updatedBy,size,page,sortBy,sortOrder}'
        Incident       | '/incidents{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,abbreviation,updatedBy,size,page,sortBy,sortOrder}'
        Scope          | '/scopes{?unit,displayName,subType,status,childElementIds,hasParentElements,hasChildElements,description,designator,name,abbreviation,updatedBy,size,page,sortBy,sortOrder,embedRisks}'
        Domain         | '/domains'
        DomainTemplate | '/domain-templates'
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

    def "target reference for #type is #reference"() {
        def entity = Stub(type) {
            firstIdAsString >> '40331ed5-be07-4c69-bf99-553811ce5454'
            secondIdAsString >> '5743c89a-5b17-4b50-8c21-72f2ac86faf3'
        }

        when:
        def result = referenceAssembler.targetReferenceOf(entity)

        then:
        result == reference

        where:
        type        | reference
        AssetRisk   | '/assets/40331ed5-be07-4c69-bf99-553811ce5454/risks/5743c89a-5b17-4b50-8c21-72f2ac86faf3'
        ProcessRisk | '/processes/40331ed5-be07-4c69-bf99-553811ce5454/risks/5743c89a-5b17-4b50-8c21-72f2ac86faf3'
        ScopeRisk   | '/scopes/40331ed5-be07-4c69-bf99-553811ce5454/risks/5743c89a-5b17-4b50-8c21-72f2ac86faf3'
    }

    def "requirement implementations reference for #type is generated"() {
        given:
        def controlImplementation = Spy(ControlImplementation) {
            owner >> Spy(type as Class<RiskAffected>) {
                idAsUUID >> UUID.fromString("aff15bfa-7259-4044-a396-59db2e16b0e0")
            }
            control >> Spy(Control) {
                idAsUUID >>  UUID.fromString("da8f6256-15e0-4fd3-a11b-4a76c916abe5")
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
        given:
        def uuid = UUID.fromString(id)
        def key = Key.from(uuid)

        expect:
        referenceAssembler.toKey(TypedId.from(uuid, type)) == key

        where:
        type     | id
        Asset    | '40331ed5-be07-4c69-bf99-553811ce5454'
        Control  | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'
        Scenario | 'f05ab334-c605-456e-8a78-9e1bc85b8509'
        Incident | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'
        Scope    | '59d3c21d-2f21-4085-950d-1273056d664a'
        Domain   | '28df429d-da5e-431a-a2d8-488c0741fb9f'
    }

    def "create multiple keys for a reference to a #type with keys #id1, #id2 "() {
        given:
        def uuid1 = UUID.fromString(id1)
        def uuid2 = UUID.fromString(id2)

        expect:
        def refs = referenceAssembler.toKeys([
            TypedId.from(uuid1, type),
            TypedId.from(uuid2, type),
        ] as Set
        )
        refs.contains(Key.from(uuid1))
        refs.contains(Key.from(uuid2))

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

    def "creates and parses URLs for identifiable #type.simpleName"() {
        given:
        def entity = Stub(type) {
            modelInterface >> type
            id >> Key.newUuid()
            idAsUUID >> it.id.value()
            if (it instanceof Profile) {
                owner >> Stub(DomainBase) {
                    id >> Key.newUuid()
                    idAsUUID >> it.id.value()
                }
            }
        }

        when: "creating a target URL"
        def targetUrl = referenceAssembler.targetReferenceOf(entity)

        then: "it can be parsed back"
        if(targetUrl != null) {
            assert referenceAssembler.parseIdentifiableRef(targetUrl) == TypedId.from(entity)
        }

        where:
        type << EntityType.TYPES.findAll { Identifiable.isAssignableFrom(it) }
    }

    def "creates and parses URLs for sym-identifiable #type.simpleName"() {
        given:
        def entity = Spy(type) {
            modelInterface >> type
            symbolicId >> Key.newUuid()
            symbolicIdAsString >> it.symbolicId.uuidValue()
            if (it instanceof CatalogItem) {
                domainBase >> Stub(Domain) {
                    modelInterface >> Domain
                    id >> Key.newUuid()
                    idAsUUID >> it.id.value()
                }
            }
            if (it instanceof ProfileItem) {
                owner >> Stub(Profile) {
                    modelInterface >> Profile
                    id >> Key.newUuid()
                    idAsUUID >> it.id.value()
                }
                domainBase >> Stub(Domain) {
                    modelInterface >> Domain
                    id >> Key.newUuid()
                    idAsUUID >> it.id.value()
                }
            }
        }

        when: "creating a target URL"
        def targetUrl = referenceAssembler.targetReferenceOf(entity)

        then: "it can be parsed back"
        if(targetUrl != null) {
            assert referenceAssembler.parseSymIdentifiableUri(targetUrl) == TypedSymbolicId.from(entity)
        }

        where:
        type << EntityType.TYPES.findAll { SymIdentifiable.isAssignableFrom(it) }
    }

    def "generates collection and search refs for #type"() {
        when: "generating collection & search URLs"
        referenceAssembler.resourcesReferenceOf(type)
        referenceAssembler.searchesReferenceOf(type)

        then:
        notThrown(Exception)

        where:
        type << EntityType.TYPES
    }

    def "parsed sym ID ref for #url is #type #id in #namespaceType #namespaceId"() {
        when:
        def ref = referenceAssembler.parseSymIdentifiableUri(url)

        then:
        ref.namespaceType == namespaceType
        ref.namespaceId.toString() == namespaceId
        ref.type == type
        ref.symbolicId == UUID.fromString(id)

        and:
        referenceAssembler.parseSymIdentifiableUri('http://localhost:9000'+url) == ref
        referenceAssembler.parseSymIdentifiableUri('http://localhost:9000/api/veo'+url) == ref

        where:
        url                                                                                                                                      | type        | id                                     | namespaceType | namespaceId
        '/domains/37aa44d5-3707-416a-864c-839f97535a06/catalog-items/a149f709-3055-40a2-867a-aaf6b2ccc36d'                                       | CatalogItem | 'a149f709-3055-40a2-867a-aaf6b2ccc36d' | Domain        | '37aa44d5-3707-416a-864c-839f97535a06'
        '/domains/37aa44d5-3707-416a-864c-839f97535a06/profiles/02557ca8-a579-4e53-a161-b1433b62eb77/items/a149f709-3055-40a2-867a-aaf6b2ccc36d' | ProfileItem | 'a149f709-3055-40a2-867a-aaf6b2ccc36d' | Profile       | '02557ca8-a579-4e53-a161-b1433b62eb77'
    }

    def "invalid URI #uri is rejected"() {
        when:
        referenceAssembler.parseIdentifiableRef(uri)

        then:
        def error = thrown UnprocessableDataException
        error.message == "Invalid entity reference: $uri"

        where:
        uri << [
            '/domains/28df429d-da5e-431a-a2d8-488c0741fb9f/templates/28df429d-da5e-431a-a2d8-488c0741fb9f',
            '/clients/28df429d-da5e-431a-a2d8-488c0741fb9f',
            'http://localhost:9000/assets/00000000-0000-0000-0000-000000000000/%252e%252e/%252e%252e/nothing',
        ]
    }

    // TODO #3039 reconsider behavior
    def "path traversal is a thing"() {
        given:
        def uri = "http://localhost:9000/assets/00000000-0000-0000-0000-000000000000/%252e%252e/%252e%252e/processes/28df429d-da5e-431a-a2d8-488c0741fb9f"

        expect:
        with(referenceAssembler.parseIdentifiableRef(uri)) {
            type == Process
            id.toString() == '28df429d-da5e-431a-a2d8-488c0741fb9f'
        }
    }
}
