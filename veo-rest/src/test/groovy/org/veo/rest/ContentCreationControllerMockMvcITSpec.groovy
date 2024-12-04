/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.rest

import static org.veo.core.entity.TailoringReferenceType.RISK
import static org.veo.rest.CompactJsonHttpMessageConverter.MEDIA_TYPE_JSON_COMPACT

import java.nio.charset.StandardCharsets

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.IncarnationLookup
import org.veo.core.entity.IncarnationRequestModeType
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.usecase.domain.DomainInUseException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.entity.jpa.ProcessRiskData

import groovy.json.JsonSlurper
/**
 * Integration test for the content creation controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class ContentCreationControllerMockMvcITSpec extends ContentSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    TransactionTemplate txTemplate

    private Domain testDomain
    private Domain completeDomain
    private Domain secondDomain
    private Domain domainSecondClient
    private Client client

    def setup() {
        txTemplate.execute {
            def rd = createRiskDefinition("id")

            this.client = createTestClient()
            newDomain(client) {
                name = "Domain 1"
            }
            newDomain(client) {
                name = "Domain 2"
            }
            newDomain(client) { d->
                name = "Domain-complete"
                newCatalogItem(d,{
                    elementType = "control"
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c1'
                })
                newCatalogItem(d,{
                    elementType = "control"
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c2'
                })
                newCatalogItem(d,{
                    elementType = "control"
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c3'
                })
                riskDefinitions = ["id":rd] as Map
            }

            client = clientRepository.save(client)

            testDomain = client.domains.find{it.name == "Domain 1"}
            completeDomain = client.domains.find{it.name == "Domain-complete"}
            secondDomain = client.domains.find{it.name == "Domain 2"}

            def secondClient = clientRepository.save(newClient() {
                newDomain(it)
            })
            domainSecondClient = secondClient.domains.first()
        }
    }

    @WithUserDetails("content-creator")
    def "create and delete a domain"() {
        when: "a domain is created"
        def domainId = parseJson(post("/content-creation/domains", [
            name:'myd1',
            authority:"myAuthority"
        ], 201)).resourceId

        then: "it can be retrieved"
        with(parseJson(get("/domains/$domainId"))) {
            name == "myd1"
            authority == "myAuthority"
        }

        when: "creating a unit using the domain"
        def unitId = parseJson(post("/units", [
            name   : "you knit",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId

        and: "trying to delete the domain"
        delete("/content-creation/domains/${domainId}", 409)

        then:
        thrown(DomainInUseException)

        when: "deleting the unit before deleting the domain"
        delete("/units/${unitId}")
        delete("/content-creation/domains/${domainId}", 204)
        get("/domains/${domainId}",404)

        then:
        thrown(NotFoundException)

        and: "only three domains remain"
        txTemplate.execute {
            clientRepository.findById(client.id).get()
        }.domains.size() == 3
    }

    @WithUserDetails("content-creator")
    def "export a domain template"() {
        given:
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)

        when: "exporting with application/json"
        def export = parseJson(get("/content-creation/domain-templates/$TEST_DOMAIN_TEMPLATE_ID"))

        then: "default and read-only values are contained"
        export.name != null
        export.profiles_v2 == []
        with(export.catalogItems.find { it.abbreviation == "cc-1" }) {
            customAspects == [:]
            with(tailoringReferences.first()) {
                target.targetUri != null
                target.id != null
                target.name != null
                target.type == "catalog-item"
            }
        }

        when: "exporting with compact media type"
        export = parseJson(get("/content-creation/domain-templates/$TEST_DOMAIN_TEMPLATE_ID", 200, MEDIA_TYPE_JSON_COMPACT))

        then: "default and read-only values are absent"
        export.name != null
        !export.containsKey("profiles_v2")
        with(export.catalogItems.find { it.abbreviation == "cc-1" }) {
            !it.containsKey("customAspects")
            with(tailoringReferences.first()) {
                target.targetUri != null
                !target.containsKey("id")
                !target.containsKey("name")
                !target.containsKey("type")
            }
        }

        when: "the compact domain template is re-imported"
        export.name = "just a copy"
        post("/content-creation/domain-templates", export)

        then:
        notThrown(Exception)

        when: "exporting with default media type"
        export = parseJson(get("/content-creation/domain-templates/$TEST_DOMAIN_TEMPLATE_ID", 200, MediaType.ALL))

        then: "non-compact format is used"
        export.profiles_v2 == []
    }

    @WithUserDetails("content-creator")
    def "update the element type schema in a domain with an object schema"() {
        given:
        testDomain.applyElementTypeDefinition(newElementTypeDefinition("scope", testDomain) {
            subTypes = [
                SCP_Scope: newSubTypeDefinition {
                    sortKey = 1
                },
                SCP_Controller: newSubTypeDefinition {
                    sortKey = 2
                },
            ]
        })
        client = clientRepository.save(client)
        testDomain = client.domains.find{it.name == "Domain 1"}
        def lastVersion = testDomain.version
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }

        when: "a request is made to the server"
        def result = post("/content-creation/domains/${testDomain.idAsString}/element-type-definitions/scope/object-schema", schemaJson, 204)

        then: "the domains are returned"
        result.andReturn().response.getContentAsString(StandardCharsets.UTF_8) == ''

        when: 'reloading the updated domain from the database'
        def updatedDomain = txTemplate.execute {
            def client = clientRepository.findById(testDomain.owningClient.get().id).get()
            def d = client.domains.find{it.id == testDomain.id}
            //initialize lazy associations
            d.elementTypeDefinitions.each {
                it.subTypes.each {
                    it.value.statuses
                }
            }
            d
        }

        then: 'the entity schemas are updated'
        updatedDomain.version > lastVersion
        with(updatedDomain.getElementTypeDefinition('scope')) {
            with(it.subTypes) {
                it.keySet() == [
                    'SCP_Scope',
                    'SCP_Processor',
                    'SCP_Controller',
                    'SCP_JointController',
                    'SCP_ResponsibleBody'
                ] as Set
                it.SCP_Scope.sortKey == 1
                it.SCP_Controller.sortKey == 2
                it.SCP_Processor.sortKey == 0
            }
            with(it.translations) {
                it.size() == 2
                with(it.get(Locale.forLanguageTag("en"))) {
                    it.scope_management == 'Head of the responsible body'
                }
            }
        }
    }

    @WithUserDetails("content-creator")
    def "trying to update the element type schema with incomplete data results in a HTTP 400"() {
        given:
        def schemaJson = [:]

        when:
        post("/content-creation/domains/${testDomain.idAsString}/element-type-definitions/scope/object-schema", schemaJson, 400)

        then:
        thrown(IllegalArgumentException)
    }

    @WithUserDetails("content-creator")
    def "update an element type definition in a domain"() {
        given:
        def lastUpdate = testDomain.updatedAt
        def lastVersion = testDomain.version

        testDomain = client.domains.find{it.name == "Domain 1"}
        def schemaJson = [
            subTypes:[
                SCP_Container:[
                    statuses:['Empty', 'Full'],
                    sortKey : 1
                ],
                SCP_Location:[
                    statuses:['Hidden', 'Revealed'],
                    sortKey : 2
                ]
            ],
            customAspects:[
                container_lid:[
                    attributeDefinitions: [
                        container_lid_present : [type: 'boolean']
                    ]
                ]
            ],
            links:[
                container_owner:[
                    targetType: 'person',
                    targetSubType: 'MasterOfDisaster',
                ]
            ],
            translations:[
                en:[
                    scope_SCP_Container_singular: 'Container',
                    scope_SCP_Container_plural: 'Containers',
                    scope_SCP_Container_status_Empty: 'Empty',
                    scope_SCP_Container_status_Full: 'Full',
                    scope_SCP_Location_singular: 'Location',
                    scope_SCP_Location_plural: 'Locations',
                    scope_SCP_Location_status_Hidden: 'Hidden',
                    scope_SCP_Location_status_Revealed: 'Revealed',
                    container_lid_present: 'Lid present?',
                    container_owner: 'Owner'
                ]
            ],
        ]

        when: "updating the scope definition"
        put("/content-creation/domains/${testDomain.idAsString}/element-type-definitions/scope", schemaJson, 204)

        and: 'reloading the updated domain from the database'
        def updatedDomain = txTemplate.execute {
            def client = clientRepository.findById(testDomain.owningClient.get().id).get()
            def d = client.domains.find{it.id == testDomain.id}
            //initialize lazy associations
            d.elementTypeDefinitions.each {
                it.subTypes.each {
                    it.value.statuses
                }
            }
            d
        }

        then: 'the entity schemas are updated'
        updatedDomain.version > lastVersion
        with(updatedDomain.getElementTypeDefinition('scope')) {
            with(it.subTypes) {
                it.keySet() ==~ [
                    'SCP_Container',
                    'SCP_Location'
                ]
                it.SCP_Location.sortKey == 2
            }
            with(it.translations) {
                it.size() == 1
                with (it[Locale.ENGLISH]) {
                    it.scope_SCP_Container_status_Empty == 'Empty'
                }
            }
        }
    }

    @WithUserDetails("content-creator")
    def "invalid attribute names are rejected"() {
        when: "updating the scope definition with space in attribute name is rejected"
        put("/content-creation/domains/${testDomain.idAsString}/element-type-definitions/scope", [
            subTypes: [
                SCP_Container: [
                    statuses: ['Empty']
                ]
            ],
            customAspects: [
                container_lid: [
                    attributeDefinitions: [
                        'container_lid present': [type: 'boolean']
                    ]
                ]
            ],
            translations: [
                en: [
                    scope_SCP_Container_status_Empty: 'Empty',
                    'container_lid present': 'Lid present?',
                ]
            ],
        ], 422)

        then:
        UnprocessableDataException ex = thrown()
        ex.message ==~ /Invalid key 'container_lid present' - .*/
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type schema as regular user"() {
        given:
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }

        when: "a request is made to the server"
        def status = postUnauthorized("/content-creation/domains/${testDomain.idAsString}/elementtypedefinitions/scope/object-schema", schemaJson)

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type definition as regular user"() {
        when:
        def response = putUnauthorized("/content-creation/domains/${testDomain.idAsString}/element-type-definitions/scope", [:])

        then:
        response.andReturn().response.status == 403
    }

    @WithUserDetails("content-creator")
    def "create catalog items in a domain from a unit"() {
        given: "a domain and a unit with elements"
        Domain domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def domainId = domain.id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId)
        def firstVersion = domain.version
        post("/domains/${domainId}/processes/${processId}/links", [
            process_requiredApplications: [
                [
                    target: [targetInDomainUri: "/domains/$domainId/assets/${assetId}"]
                ]
            ]
        ], 204)

        def scenarioPart = parseJson(post("/domains/$domainId/scenarios", [
            name: "example scenario 1",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            parts: [
                [targetInDomainUri: "/domains/$domainId/scenarios/$scenarioId"]
            ]
        ],201)).resourceId

        post("/domains/$domainId/scenarios", [
            name: "example scenario Container",
            abbreviation: "Cont",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            parts: [
                [targetInDomainUri: "/domains/$domainId/scenarios/$scenarioPart"]
            ]
        ],201)

        def scopeMember1Id = parseJson(post("/domains/$domainId/assets", [
            name: "asset scope member 1",
            abbreviation: "a1",
            subType: "AST_Application",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
        ],201)).resourceId

        post("/domains/$domainId/scopes", [
            name: "example scope",
            abbreviation: "Cont",
            subType: "SCP_Scope",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            members: [
                [targetInDomainUri: "/domains/$domainId/assets/$scopeMember1Id"]
            ]
        ],201)

        when: "we create new catalog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=${unitId}",
                [:], 204)

        domain = txTemplate.execute {
            domainDataRepository.findById(domainId).get().tap {
                it.catalogItems*.tailoringReferences*.size()
            }
        }
        def newVersion = domain.version

        then:
        newVersion > firstVersion

        when:
        def catalogItems = domain.catalogItems

        then:
        catalogItems.size() == 12
        with(catalogItems.sort{it.name}[0]) {
            name == "asset"
            elementType == "asset"
            status == "NEW"
            subType == "AST_Application"
            abbreviation == null
            tailoringReferences.size() == 1
            tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
            tailoringReferences[0].linkType == "process_requiredApplications"
        }

        with(catalogItems.find{it.name == "example scenario Container" }) {
            elementType == "scenario"
            status == "NEW"
            subType == "SCN_Scenario"
            abbreviation == "Cont"
            tailoringReferences.size() == 1
            tailoringReferences[0].referenceType == TailoringReferenceType.PART
        }

        with(catalogItems.find{it.name == "example scope" }) {
            elementType == "scope"
            status == "NEW"
            subType == "SCP_Scope"
            abbreviation == "Cont"
            tailoringReferences.size() == 1
            tailoringReferences[0].referenceType == TailoringReferenceType.MEMBER
        }

        with(catalogItems.find{it.name == "asset scope member 1" }) {
            elementType == "asset"
            status == "NEW"
            subType == "AST_Application"
            tailoringReferences.size() == 1
            tailoringReferences[0].referenceType == TailoringReferenceType.SCOPE
        }

        when: "we incarnate one linked catalog item"
        def catalogItemsId = catalogItems.find{it.name == "example scenario Container" }.symbolicIdAsString
        unitId = parseJson(post("/units", [
            name   : "you knit 2",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId
        def incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$catalogItemsId"))
        def elementList = parseJson(post("/units/${unitId}/incarnations", incarnationDescription))

        then: "all linked elements are created"
        incarnationDescription.parameters.size() == 3
        elementList.size() == 3

        when: "we incarnate all catalog items"
        delete("/units/${unitId}")
        def catalogItemsIds = catalogItems.collect{it.symbolicIdAsString}.join(',')
        unitId = parseJson(post("/units", [
            name   : "you knit 2",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId
        incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$catalogItemsIds"))
        elementList = parseJson(post("/units/${unitId}/incarnations", incarnationDescription))

        assetId = elementList.find {it.targetUri.contains('assets')}.targetUri.split('/' ).last()
        def asset = txTemplate.execute {
            assetDataRepository.findById(UUID.fromString(assetId)).get()
        }

        def scenarioIds = elementList.collect {it.targetUri}.findAll {it.contains('scenarios')}.collect {it.split('/' ).last()}
        def scenarios = txTemplate.execute {
            def list = scenarioDataRepository.findAllWithCompositesAndCompositesPartsByIdIn(scenarioIds)
            list.collect { it.appliedCatalogItems.size() }
            list.collect { it.parts.size() }
            list
        }

        then: "the item is applied"
        asset.appliedCatalogItems.size() == 1

        with(scenarios.find{ it.name == "example scenario Container"}) {
            parts.size() == 1
            parts[0].composites.size() == 1
            parts[0].composites[0] == it
            appliedCatalogItems.size() == 1
        }

        when: "we create new catalog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=${unitId}",
                [:], 204)

        asset = txTemplate.execute {
            assetDataRepository.findById(UUID.fromString(assetId)).get()
        }

        then: "the reference to the updated catalog item is intact"
        asset.appliedCatalogItems.size() == 1

        when:
        domain = txTemplate.execute {
            domainDataRepository.findById(domainId).get().tap {
                it.catalogItems*.tailoringReferences*.size()
            }
        }
        def finalVersion = domain.version

        then:
        finalVersion > newVersion

        when: "we incarnate scenario 1 and link the composite feature to the first scenario"
        catalogItems = catalogItems = domain.catalogItems

        def scenarioItemIds = catalogItems.find{ it.name =="example scenario 1" }.collect {it.symbolicIdAsString}.join(',')
        incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$scenarioItemIds&mode=MANUAL"))
        def references = incarnationDescription.parameters.references.first()

        then:
        incarnationDescription.parameters.size() == 1
        references.size() == 2
        references*.referenceType ==~ ["PART", "COMPOSITE"]

        when: "we link the composite to an existing scenario"
        def existingScenarioInNewUnitId = parseJson(post("/domains/$domainId/scenarios", [
            name: "existing scenario in new unit",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ],201)).resourceId
        references[0].put("referencedElement", ["targetUri": "/scenarios/$existingScenarioInNewUnitId"])
        references[1].put("referencedElement", ["targetUri": "/scenarios/$existingScenarioInNewUnitId"])
        elementList = parseJson(post("/units/${unitId}/incarnations", incarnationDescription))

        def modifiedScenario = txTemplate.execute {
            scenarioDataRepository.findById(UUID.fromString(existingScenarioInNewUnitId)).get().tap {
                parts.collect { it.parts.size() }
                composites.collect { it.composites.size() }
            }
        }
        def newScenario1 = txTemplate.execute {
            def q = scenarioDataRepository.findById(UUID.fromString(elementList[0].targetUri.split('/' ).last())).get()
                    .tap {
                        parts.collect { it.parts.size() }
                        composites.collect { it.composites.size() }
                    }
        }

        then: "the existing scenario has the new as part and the new scenario a composite"
        elementList.size() == 1
        modifiedScenario.parts.size() == 1
        modifiedScenario.parts[0] == newScenario1
        newScenario1.composites.size() == 1
        newScenario1.composites[0] == modifiedScenario

        when: "we only include PART"
        incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$scenarioItemIds&mode=MANUAL&include=PART"))
        references = incarnationDescription.parameters.references.first()

        then: "only PART is returned"
        incarnationDescription.parameters.size() == 1
        references.size() == 1
        references.referenceType == ["PART"]

        when: "we exclude PART"
        incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$scenarioItemIds&mode=MANUAL&exclude=PART"))
        references = incarnationDescription.parameters.references.first()

        then: "only COMPOSITE is returned"
        incarnationDescription.parameters.size() == 1
        references.size() == 1
        references.referenceType == ["COMPOSITE"]
    }

    @WithUserDetails("content-creator")
    def "create an empty profile in a domain"() {
        given: "a domain"
        def domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def domainId = domain.id
        def firstVersion = domain.version

        when: "we create a new empty profile"
        post("/content-creation/domains/${domainId}/profiles",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE',
                    productId: 'TEST',
                ], 201)

        Domain domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.size()
            d
        }
        def newVersion = domain1.version

        then: "the profile is created"
        newVersion > firstVersion
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test"
            description == "All the good stuff"
            language == "de_DE"
            productId == "TEST"
        }

        when: "we update the empty profile"
        def profileId = domain1.getProfiles().first().idAsString
        put("/content-creation/domains/${domainId}/profiles/${profileId}",
                [
                    name: 'test1',
                    description: 'All the good stuff, but better.',
                    language: 'de_DE',
                    productId: 'TEST_1',
                ], 204)

        domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.size()
            d
        }
        def thirdVersion = domain1.version

        then: "the profile is updated"
        thirdVersion > newVersion
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test1"
            description == "All the good stuff, but better."
            language == "de_DE"
            productId == "TEST_1"
        }

        when: "deleting the profile"
        delete("/content-creation/domains/$domainId/profiles/$profileId")

        then: "it is gone"
        txTemplate.execute {
            domainDataRepository.findById(domainId).get().profiles.empty
        }
    }

    @WithUserDetails("content-creator")
    def "update incarnation config"() {
        when:
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", [
            mode: "MANUAL",
            useExistingIncarnations: "NEVER",
            include: ["LINK", "LINK_EXTERNAL"],
        ], 204)

        then:
        with(parseJson(get("/domains/$testDomain.idAsString/incarnation-configuration"))) {
            size() == 3
            mode == "MANUAL"
            useExistingIncarnations == "NEVER"
            include ==~ ["LINK", "LINK_EXTERNAL"]
        }

        when:
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", [
            mode: "DEFAULT",
            useExistingIncarnations: "ALWAYS",
            exclude: ["LINK", "LINK_EXTERNAL"],
        ], 204)

        then:
        with(parseJson(get("/domains/$testDomain.idAsString/incarnation-configuration"))) {
            size() == 3
            mode == "DEFAULT"
            useExistingIncarnations == "ALWAYS"
            exclude ==~ ["LINK", "LINK_EXTERNAL"]
        }
    }

    @WithUserDetails("content-creator")
    def "compact incarnation config representation can be put back with #mode & #lookup"() {
        given: "a persisted config"
        def config = [
            mode: "$mode",
            useExistingIncarnations: "$lookup"
        ]
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", config, 204)

        when: "fetching and putting back a compact representation"
        def compactConfig = parseJson(get("/domains/$testDomain.idAsString/incarnation-configuration", 200, MEDIA_TYPE_JSON_COMPACT))
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", compactConfig, 204)

        then: "the values haven't changed"
        notThrown(Exception)
        parseJson(get("/domains/$testDomain.idAsString/incarnation-configuration", 200)) == config

        where:
        [mode, lookup] << [
            IncarnationRequestModeType.values(),
            IncarnationLookup.values()
        ].combinations()
    }

    @WithUserDetails("content-creator")
    def "incarnation config is validated"() {
        when: "omitting useExistingIncarnations"
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", [
            mode: "MANUAL",
            exclude: ["LINK", "LINK_EXTERNAL"],
        ], 400)

        then:
        def ex = thrown(HttpMessageNotReadableException)
        ex.message.endsWith "useExistingIncarnations is marked non-null but is null"

        when: "omitting mode"
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", [
            useExistingIncarnations: "ALWAYS",
            exclude: ["LINK", "LINK_EXTERNAL"],
        ], 400)

        then:
        ex = thrown(HttpMessageNotReadableException)
        ex.message.endsWith "mode is marked non-null but is null"

        when: "trying to combine allow-list and deny-list"
        put("/content-creation/domains/$testDomain.idAsString/incarnation-configuration", [
            mode: "MANUAL",
            useExistingIncarnations: "NEVER",
            include: ["LINK", "LINK_EXTERNAL"],
            exclude: ["SCOPE"],
        ], 422)

        then:
        ex = thrown(HttpMessageNotReadableException)
        ex.message.endsWith "Cannot combine include and exclude lists (at least one of them must be null)"
    }

    @WithUserDetails("content-creator")
    def "update a profile in a domain"() {
        given: "a domain and a unit"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id
        def unitId = createUnitWithElements(domainId).first()

        when: "we create a new profile"
        post("/content-creation/domains/${domainId}/profiles?unit=${unitId}",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE',
                    productId: 'TEST',
                ], 201)

        Domain domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.each {
                it.items.size()
                it.items.each{
                    it.tailoringReferences.size()
                }
            }
            d
        }

        then: "the profile is created"
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test"
            description == "All the good stuff"
            language == "de_DE"
            productId == "TEST"
            items.size() == 8
        }

        when: "we update the profile meta data"
        def profileId = domain1.getProfiles().first().idAsString
        put("/content-creation/domains/${domainId}/profiles/${profileId}",
                [
                    name: 'test1',
                    description: 'All the good stuff, but better.',
                    language: 'de_DE',
                    productId: "TEST_1",
                ], 204)

        domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.each {
                it.items.size()
                it.items.each{
                    it.tailoringReferences.size()
                }
            }
            d
        }

        then: "the profile meta data is updated"
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test1"
            description == "All the good stuff, but better."
            language == "de_DE"
            productId == "TEST_1"
            items.size() == 8
        }

        when: "we create another element in the unit"
        post("/domains/$domainId/scenarios", [
            name: "example scenario 1",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ],201)

        and: "update the profile"
        put("/content-creation/domains/${domainId}/profiles/${profileId}?unit=${unitId}",
                [
                    name: 'test1',
                    description: 'All the good stuff, but much better.',
                    language: 'de_DE',
                    productId: 'TEST_1',
                ], 204)

        domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.each {
                it.items.size()
                it.items.each{
                    it.tailoringReferences.size()
                }
            }
            d
        }

        then: "the profile is updated"
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test1"
            description == "All the good stuff, but much better."
            language == "de_DE"
            productId == "TEST_1"
            items.size() == 9
        }
    }

    @WithUserDetails("content-creator")
    def "export and import a profile from a domain"() {
        given: "a domain and a unit"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId, true)

        when: "we incarnate one linked catalog item"
        def catalogItems = txTemplate.execute {
            domainDataRepository.findById(domainId).get().catalogItems.each {
                it.tailoringReferences.size()
            }
        }
        def catalogItemsId = catalogItems.find{it.name == "Control-2" }.symbolicIdAsString

        def incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$catalogItemsId"))
        post("/units/${unitId}/incarnations", incarnationDescription)

        and: "we link the process with the asset"
        post("/domains/$domainId/processes/$processId/links", [
            process_requiredApplications: [
                [
                    target: [targetUri: "/assets/$assetId"]
                ]
            ]
        ], 204)

        and: "we create a simple circle in the profile by linking two controls"
        def c1 = parseJson(post("/controls", [
            name   : "control-1",
            domains: [
                (domainId): [
                    subType: "CTL_TOM",
                    status: "NEW",
                ]
            ],
            owner  : [targetUri: "/units/$unitId"]
        ])).resourceId

        def c2 = parseJson(post("/controls", [
            name   : "control-2",
            domains: [
                (domainId): [
                    subType: "CTL_TOM",
                    status: "NEW",
                ]
            ],
            owner  : [targetUri: "/units/$unitId"]
        ])).resourceId

        post("/domains/$domainId/controls/$c1/links", [
            control_tom: [
                [
                    target: [targetUri: "/controls/$c2"]
                ]
            ]
        ], 204)

        post("/domains/$domainId/controls/$c2/links", [
            control_tom: [
                [
                    target: [targetUri: "/controls/$c1"]
                ]
            ]
        ], 204)

        and: "we create a new profile and export"
        def profileId = parseJson(post("/content-creation/domains/${domainId}/profiles?unit=${unitId}",
                [
                    name: 'export-test',
                    description: 'All the good stuff',
                    language: 'de_DE',
                    productId: 'EXPORT_TEST',
                ], 201)).id

        def exportedProfile = parseJson(get("/domains/${domainId}/profiles/${profileId}/export"))

        then: "the profile is exported"
        with(exportedProfile) {
            name == 'export-test'
            description == 'All the good stuff'
            productId == 'EXPORT_TEST'
            items.size() == 11
        }
        with(exportedProfile.items.find{it.name == "Control-2" }) {
            abbreviation == 'c-2'
            appliedCatalogItem.name == 'Control-2'
            appliedCatalogItem.namespaceId == domainId.toString()
        }
        with(exportedProfile.items.find{it.name == "process" }) {
            tailoringReferences.size() == 2
            tailoringReferences.referenceType ==~  ['LINK', 'RISK']
        }

        when: "we post the exported profile to the domain template"
        exportedProfile.name = 'export-test1'
        def profileInTemplateId = parseJson(post("/content-creation/domain-templates/${DSGVO_TEST_DOMAIN_TEMPLATE_ID}/profiles",
                exportedProfile)).id

        def dt = txTemplate.execute {
            domainTemplateDataRepository.findById(DSGVO_TEST_DOMAIN_TEMPLATE_ID).get().tap{dt1->
                dt1.profiles.size()
                dt1.profiles[0].items.size()
                dt1.profiles[0].items.size()
                dt1.profiles[0].items.each{
                    it.name
                    it.tailoringReferences.size()
                    it.appliedCatalogItem?.getName()
                    it.appliedCatalogItem?.getNamespace()
                }
            }
        }

        then:
        profileInTemplateId != null
        dt.profiles.size() == 1
        dt.profiles[0].name == 'export-test1'
        dt.profiles[0].items.size() == 11

        with(dt.profiles[0].items.find{it.name == "Control-2" }) {
            abbreviation == 'c-2'
            appliedCatalogItem.name == 'Control-2'
            appliedCatalogItem.getNamespace() == dt
        }

        when: "post the exported profile again to override"
        exportedProfile.description = 'a new description'
        profileInTemplateId = parseJson(post("/content-creation/domain-templates/${DSGVO_TEST_DOMAIN_TEMPLATE_ID}/profiles",
                exportedProfile)).id
        dt = txTemplate.execute {
            domainTemplateDataRepository.findById(DSGVO_TEST_DOMAIN_TEMPLATE_ID).get().tap{dt1->
                dt1.profiles.size()
                dt1.profiles[0].items.size()
                dt1.profiles[0].items.each{
                    it.name
                    it.tailoringReferences.size()
                    if(it.appliedCatalogItem != null) {
                        it.appliedCatalogItem.getName()
                        it.appliedCatalogItem.getNamespace()
                    }
                }
            }
        }

        then:
        profileInTemplateId != null
        dt.profiles.size() == 1
        dt.profiles[0].name == 'export-test1'
        dt.profiles[0].description == 'a new description'
        dt.profiles[0].items.size() == 11

        with(dt.profiles[0].items.find{it.name == "Control-2" }) {
            abbreviation == 'c-2'
            appliedCatalogItem.name == 'Control-2'
            appliedCatalogItem.getNamespace() == dt
        }

        when:
        delete("/content-creation/domain-templates/$DSGVO_TEST_DOMAIN_TEMPLATE_ID/profiles/$profileInTemplateId")
        dt = txTemplate.execute {
            domainTemplateDataRepository.findById(DSGVO_TEST_DOMAIN_TEMPLATE_ID).get().tap{
                profiles.size()
            }
        }

        then:
        dt.profiles.empty
    }

    @WithUserDetails("content-creator")
    def "import a profile into a domain template"() {
        given: "an empty domain template"
        def domainTemplate = parseJson(post("/content-creation/domains/${testDomain.idAsString}/template",[version : "1.0.0"]))

        when: "we add a profile"
        def result = parseJson(post("/content-creation/domain-templates/${domainTemplate.id}/profiles", [
            "name" : "export-test",
            "id": "${UUID.randomUUID()}",
            "description" : "All the good stuff",
            "language" : "de_DE",
            "productId": "EXPORT_TEST",
            "items" : []
        ]))

        then:
        result != null

        when: "loading the domain templates from the database"
        def dt = txTemplate.execute {
            domainTemplateRepository.findAll().find{ it.name == "Domain 1" }.tap{
                profiles.size()
            }
        }

        then: "the profile exist"
        dt.profiles.size() == 1

        when: "we overwrite the profile"
        result = parseJson(post("/content-creation/domain-templates/${domainTemplate.id}/profiles", [
            "name" : "export-test improved",
            "id": "${UUID.randomUUID()}",
            "description" : "All the good stuff",
            "language" : "de_DE",
            "productId" : "EXPORT_TEST",
            "items" : [
                [
                    "name" : "asset1",
                    "id": "${UUID.randomUUID()}",
                    "elementType" : "asset",
                    "subType" : "AST_Application",
                    "status" : "NEW",
                ],
                [
                    "name" : "asset2",
                    "id": "${UUID.randomUUID()}",
                    "elementType" : "asset",
                    "subType" : "AST_Application",
                    "status" : "NEW",
                ],
            ]
        ]))

        then:
        result != null

        when: "loading the domain templates from the database"
        dt = txTemplate.execute {
            domainTemplateRepository.findAll().find{ it.name == "Domain 1" }.tap{
                it.profiles.size()
                it.profiles[0].items.size()
            }
        }

        then: "the profile exist"
        dt.profiles.size() == 1
        dt.profiles[0].productId == 'EXPORT_TEST'
        dt.profiles[0].items.size() == 2
        dt.profiles[0].items.name ==~ ['asset1', 'asset2']
    }

    @WithUserDetails("content-creator")
    def "create profile in a domain from a unit"() {
        given: "a domain and a unit with elements"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId, true)

        post("/domains/${domainId}/processes/${processId}/links", [
            process_requiredApplications: [
                [
                    target: [targetInDomainUri: "/domains/$domainId/assets/${assetId}"]
                ]
            ]
        ], 204)

        def scenarioPart = parseJson(post("/domains/$domainId/scenarios", [
            name: "example scenario 1",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ],201)).resourceId

        post("/domains/$domainId/scenarios", [
            name: "example scenario Container",
            abbreviation: "Cont",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            parts: [
                [targetInDomainUri: "/domains/$domainId/scenarios/$scenarioId"],
                [targetInDomainUri: "/domains/$domainId/scenarios/$scenarioPart"],
            ]
        ],201)

        when: "we create a new profile from the unit"
        def profileRef = parseJson(post("/content-creation/domains/${domainId}/profiles?unit=${unitId}",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE',
                    productId: 'TEST',
                ], 201))

        then: "the resource is returned"
        with(profileRef) {
            displayName == "test"
            targetUri.contains("domains/${domainId}/profiles/")
        }

        when: "we select the data in the db"
        def domain1 = txTemplate.execute {
            domainDataRepository.findById(domainId).get().tap{d->
                d.profiles.each {
                    it.items.size()
                    it.items.each{
                        it.tailoringReferences.size()
                    }
                }
            }
        }

        and: "we export the original unit to compare later"
        def exportedOrgUnit = parseJson(get("/units/${unitId}/export"))

        then:
        domain1.profiles.size() == 1
        domain1.profiles[0].items.size() == 10
        with(domain1.profiles[0].items.sort{it.name}[0]) {
            name == "asset"
            elementType == "asset"
            status == "NEW"
            subType == "AST_Application"
            abbreviation == null
            tailoringReferences.size() == 2
            with(tailoringReferences.sort {it.referenceType}) {
                it[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
                it[0].linkType == "process_requiredApplications"
                it[1].referenceType == RISK
            }
        }
        with(domain1.profiles[0].items.find{it.name == "example scenario Container" }) {
            elementType == "scenario"
            status == "NEW"
            subType == "SCN_Scenario"
            abbreviation == "Cont"
            tailoringReferences.size() == 2
            tailoringReferences[0].referenceType == TailoringReferenceType.PART
        }

        when: "we get the profile"
        def profiles = parseJson(get("/domains/${domain1.idAsString}/profiles"))

        then: "the profile is returned"
        profiles.size() == 1
        with(profiles[0]) {
            name == 'test'
            description == 'All the good stuff'
            language == 'de_DE'
            productId == 'TEST'
        }

        when: "we get the profile items"
        def profileItems = parseJson(get("/domains/${domain1.idAsString}/profiles/${profiles[0].id}/items"))

        then:
        profileItems.size() == 10
        with(profileItems.sort{it.name}[0]) {
            name == "asset"
            elementType == "asset"
            subType == "AST_Application"
            abbreviation == null
        }

        when: "we create a new unit"
        unitId = parseJson(post("/units", [
            name   : "applied profile unit",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId

        and: "applying the profile to it"
        post("/domains/${domain1.idAsString}/profiles/${domain1.profiles[0].idAsString}/incarnation?unit=${unitId}",[:], 204)

        and: "export the unit"
        def exportedUnit = parseJson(get("/units/${unitId}/export"))

        then: "both exports are the same"
        exportedUnit.elements.size() == exportedOrgUnit.elements.size()
        exportedUnit.risks.size() == exportedOrgUnit.risks.size()
        exportedUnit.elements.forEach{s->
            with(exportedOrgUnit.elements.find{it.name == s.name}) {
                type == s.type
                domains == s.domains
                links.size() == s.links.size()
                customAspects == s.customAspects
            }
        }

        when: "exporting the domain"
        def exportedDomain = parseJson(get("/domains/${domain1.idAsString}/export"))

        then: "the profiles are exported"
        exportedDomain != null
        exportedDomain.profiles_v2.size() ==1
        with(exportedDomain.profiles_v2.first()) {
            name == "test"
            items.size() == 10
            with(items.find { it.name == 'asset' } ) {
                elementType=="asset"
                subType=="AST_Application"
                tailoringReferences.size() == 2
            }
        }

        when: "we applying a non existent profile to the unit"
        post("/domains/${domain1.idAsString}/profiles/${UUID.randomUUID()}/incarnation?unit=${unitId}",[:], 404)

        then: "it is not found"
        thrown(NotFoundException)

        when: "we applying an existent profile to the unit with random domain"
        post("/domains/${UUID.randomUUID()}/profiles/${domain1.profiles[0].idAsString}/incarnation?unit=${unitId}",[:], 404)

        then: "it is not found"
        thrown(NotFoundException)
    }

    @WithUserDetails("content-creator")
    def "profile product ID + language must be unique"() {
        given:
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID).id

        when: "trying to use an existing product ID + language"
        post("/content-creation/domains/$domainId/profiles", [
            name: "Neue Beispielorganisation",
            productId: "EXAMPLE_ORGANIZATION",
            language: "de_DE",
        ], 409)

        then:
        def creationEx = thrown(Exception)
        creationEx.message == "A profile with product ID 'EXAMPLE_ORGANIZATION' in language 'de_DE' already exists in domain 'DS-GVO'"

        expect: "a different language to work"
        def newProfileId = parseJson(post("/content-creation/domains/$domainId/profiles", [
            name: "New example organization",
            productId: "EXAMPLE_ORGANIZATION",
            language: "en_US",
        ])).id

        when: "trying to update the profile with the original profile's language"
        put("/content-creation/domains/$domainId/profiles/$newProfileId", [
            name: "Sprachwechsel leicht gemacht",
            productId: "EXAMPLE_ORGANIZATION",
            language: "de_DE",
        ], 409)

        then:
        def updateEx = thrown(Exception)
        updateEx.message == "A profile with product ID 'EXAMPLE_ORGANIZATION' in language 'de_DE' already exists in domain 'DS-GVO'"

        expect: "a different product ID to work"
        put("/content-creation/domains/$domainId/profiles/$newProfileId", [
            name: "Sprachwechsel leicht gemacht",
            productId: "LANGUAGE_SWITCH_NOW_EASY",
            language: "de_DE",
        ], 204)
    }

    @WithUserDetails("content-creator")
    def "create a profile in a domain from a unit with applied items "() {
        given: "a domain and a unit with elements"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId)

        post("/domains/${domainId}/processes/${processId}/links", [
            process_requiredApplications: [
                [
                    target: [targetInDomainUri: "/domains/$domainId/assets/${assetId}"]
                ]
            ]
        ], 204)

        when: "we create new catalog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=${unitId}",
                [:], 204)

        def catalogItems = txTemplate.execute {
            domainDataRepository.findById(domainId).get().catalogItems.each {
                it.tailoringReferences.size()
            }
        }

        then:
        catalogItems.size() == 8
        with(catalogItems.sort{it.name}[0]) {
            name == "asset"
            elementType == "asset"
            status == "NEW"
            subType == "AST_Application"
            abbreviation == null
            tailoringReferences.size() == 1
            tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
            tailoringReferences[0].linkType == "process_requiredApplications"
        }

        when: "we incarnate all catatlog items"
        def catalogItemsIds = catalogItems.collect{it.symbolicIdAsString}.join(',')
        unitId = parseJson(post("/units", [
            name   : "you knit 2",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId
        def incarnationDescription = parseJson(get("/units/$unitId/domains/$domainId/incarnation-descriptions?itemIds=$catalogItemsIds"))
        def elementList = parseJson(post("/units/${unitId}/incarnations", incarnationDescription))
        assetId = elementList.find {it.targetUri.contains('assets')}.targetUri.split('/' ).last()
        scenarioId = elementList.find {it.targetUri.contains('scenarios')}.targetUri.split('/' ).last()
        def personId = elementList.find {it.targetUri.contains('persons')}.targetUri.split('/' ).last()
        def controlId = elementList.find {it.targetUri.contains('controls')}.targetUri.split('/' ).last()

        post("/assets/$assetId/risks", [
            domains : [
                (domainId): [
                    reference: [targetUri: "http://localhost/domains/$domainId"]
                ]
            ],
            riskOwner: [targetUri: "http://localhost/persons/$personId"],
            mitigation: [targetUri: "http://localhost/controls/$controlId"],
            scenario: [targetUri: "http://localhost/scenarios/$scenarioId"]
        ])

        and: "we create a new profile from the unit"
        def profile = parseJson(post("/content-creation/domains/${domainId}/profiles?unit=$unitId",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE'
                ], 201))

        def domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.each {
                it.items.size()
                it.items.each{
                    it.tailoringReferences.size()
                    it.appliedCatalogItem.name
                }
            }
            d
        }

        then: "the profile ist created and complete and persistent"
        domain1.profiles.size() == 1
        profile.targetUri.endsWith(domain1.profiles[0].idAsString)
        with(domain1.profiles[0].items.sort{it.name}[0]) {
            name == "asset"
            elementType == "asset"
            status == "NEW"
            subType == "AST_Application"
            abbreviation == null
            appliedCatalogItem.name == "asset"
            tailoringReferences.size() == 3
            with(tailoringReferences.sort {it.referenceType}) {
                it[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
                it[0].linkType == "process_requiredApplications"
                it[1].referenceType == RISK
                it[1].mitigation.name == "control"
                it[1].riskOwner.name == "person"
                it[1].target.name == "scenario"
                it[2].referenceType == TailoringReferenceType.CONTROL_IMPLEMENTATION
                it[2].target.name == "control"
            }
        }
    }

    @WithUserDetails("content-creator")
    def "risks are not supported for catalog items"() {
        given: "a unit with risks"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId, true)

        when: "trying to create catalog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=${unitId}", [:], 422)

        then: "it fails"
        UnprocessableDataException ex = thrown()
        ex.message == "Risks currently not supported for catalog items"
    }

    @WithUserDetails("content-creator")
    def "create a domain template with unit"() {
        Domain domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def unitId = createUnitWithElements(domain.id, true).first()

        given: "a number of existing templates"
        def initialTemplateCount = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        when: "a profile and template are created"
        post("/content-creation/domains/$domain.idAsString/profiles?unit=$unitId", [
            name: 'Example elements',
            description: 'All the good stuff',
            language: 'de_DE',
            productId: 'EXAMPLE_ELEMENTS',
        ])
        def result = parseJson(post("/content-creation/domains/${domain.idAsString}/template", [
            version : "1.1.0"
        ]))

        then: "a result is returned"
        result != null

        and: "there is one more template in the repo"
        domainTemplateDataRepository.count() == initialTemplateCount + 1

        when: "loading the domain templates from the database"
        def dt = txTemplate.execute {
            domainTemplateRepository.findAll()
                    .find{ it.name == domain.name && it.templateVersion == "1.1.0"}
                    .tap{
                        it.profiles*.items*.tailoringReferences*.id
                    } // init proxy
        }

        then: "the template is found, the version is set"
        dt.templateVersion == "1.1.0"

        and: "the example profile exists"
        with(dt.profiles.find { it.name == "Example elements" }) {
            description == 'All the good stuff'
            language == 'de_DE'
            productId == 'EXAMPLE_ELEMENTS'

            items*.elementType ==~ [
                "asset",
                "control",
                "document",
                "incident",
                "person",
                "process",
                "scenario",
                "scope"
            ]
            items.collectMany { it.tailoringReferences }.findAll { it.referenceType == RISK }.size() == 2
        }

        when: "creating and exporting the domain"
        Domain newDomain = createTestDomain(client, dt.id)
        def results = get("/domains/${newDomain.idAsString}/export")
        def exportedDomain = parseJson(results)

        then:" the export file contains the profile data"
        exportedDomain.name == newDomain.name
        with(exportedDomain.profiles_v2.find { it.name == "Example elements" }) {
            items*.elementType ==~ [
                "asset",
                "control",
                "document",
                "incident",
                "person",
                "process",
                "scenario",
                "scope"
            ]
            items.collectMany { it.tailoringReferences }.findAll { it.referenceType == "RISK" }.size() == 2
        }

        when: "we create a new domain template from the export"
        exportedDomain.templateVersion = "1.2.4"
        def domainTemplateId = parseJson(post("/content-creation/domain-templates", exportedDomain)).resourceId

        then: "the domain template is created"
        UUID.fromString(domainTemplateId)

        when: "we create a domain from the domain template"
        newDomain = createTestDomain(client, UUID.fromString(domainTemplateId))

        then: "the domain contains the profiles"
        newDomain.name == "DSGVO-test"
        with(newDomain.profiles.find { it.name == "Example elements" }) {
            items*.elementType ==~ [
                "asset",
                "control",
                "document",
                "incident",
                "person",
                "process",
                "scenario",
                "scope"
            ]
            items.collectMany { it.tailoringReferences }.findAll { it.referenceType == RISK }.size() == 2
        }
    }

    @WithUserDetails("content-creator")
    def "risk definition from domain template can be deleted"() {
        given:
        def dsgvoId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID).idAsString
        def dsgvoUpdatedAt = parseJson(get("/domains/$dsgvoId")).updatedAt

        when: "deleting the risk definition"
        delete("/content-creation/domains/$dsgvoId/risk-definitions/DSRA")

        then: "it's gone"
        with(parseJson(get("/domains/$dsgvoId"))) {
            riskDefinitions == [:]
            updatedAt > dsgvoUpdatedAt
        }
    }

    @WithUserDetails("user@domain.example")
    def "create a domain template forbidden for user"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def status = postUnauthorized("/content-creation/domains/${testDomain.idAsString}/template", [version : "1.0.0"])

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }

    @WithUserDetails("content-creator")
    def "remove risk value matrix from a risk category"() {
        given:
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID).id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId, true, true)

        when:
        def domain = domainDataRepository.findByIdWithProfilesAndRiskDefinitions(domainId, client.id).get()
        def riskDefinition = domain.riskDefinitions.DSRA

        then:
        with(riskDefinition.categories) {
            it.size() == 4
            it.find{it.id == 'C'}?.riskValuesSupported
        }

        when:
        def process = processDataRepository.findWithRisksAndScenariosByIdIn([processId]).first()
        def risk = process.risks.first() as ProcessRiskData

        then:
        with(process.riskValuesAspects) {
            it.size() == 1
            with(first()) {
                it.values.size() == 1
                with (it.values.entrySet().first()) {
                    it.key.idRef == 'DSRA'
                    with (it.value.potentialImpacts) {
                        it.keySet()*.idRef ==~ ['C', 'I']
                    }
                }
            }
        }
        with(risk.riskAspects) {
            it.size() == 1
            with(first()) {
                with(it.impactCategories) {
                    it*.category*.idRef ==~ ['C', 'I', 'A', 'R']
                }
                with(it.riskCategories) {
                    it*.category*.idRef ==~ ['C', 'I', 'A', 'R']
                }
            }
        }

        when:
        def riskDefinitionJson = parseJson(get("/domains/$domainId")).with {
            it.riskDefinitions.DSRA
        }
        riskDefinitionJson.categories.find{it.id == 'C'}.valueMatrix = null
        put("/content-creation/domains/${domain.idAsString}/risk-definitions/DSRA", riskDefinitionJson)
        domain = domainDataRepository.findByIdWithProfilesAndRiskDefinitions(domainId, client.id).get()
        riskDefinition = domain.riskDefinitions.DSRA

        then:
        with(riskDefinition.categories) {
            it.size() == 4
            !it.find{it.id == 'C'}.riskValuesSupported
        }

        when:
        process = processDataRepository.findWithRisksAndScenariosByIdIn([processId]).first()
        risk = process.risks.first() as ProcessRiskData

        then:
        with(process.riskValuesAspects) {
            it.size() == 1
            with(first()) {
                it.values.size() == 1
                with (it.values.entrySet().first()) {
                    it.key.idRef == 'DSRA'
                    with (it.value.potentialImpacts) {
                        it.keySet()*.idRef ==~ ['C', 'I']
                    }
                }
            }
        }
        with(risk.riskAspects) {
            it.size() == 1
            with(first()) {
                with(it.impactCategories) {
                    it*.category*.idRef ==~ ['I', 'A', 'R']
                }
                with(it.riskCategories) {
                    it*.category*.idRef ==~ ['I', 'A', 'R']
                }
            }
        }
    }

    @WithUserDetails("content-creator")
    def "add risk category"() {
        given:
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID).id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId, true, true)

        when:
        def domain = domainDataRepository.findByIdWithProfilesAndRiskDefinitions(domainId, client.id).get()
        def riskDefinition = domain.riskDefinitions.DSRA
        def rdRef = RiskDefinitionRef.from(riskDefinition)

        then:
        with(riskDefinition.categories) {
            it.size() == 4
        }

        when:
        def riskDefinitionJson = parseJson(get("/domains/$domainId")).with {
            it.riskDefinitions.DSRA
        }
        riskDefinitionJson.categories << riskDefinitionJson.categories.find{it.id == 'C'}.clone().tap {
            id = 'C2'
        }
        put("/content-creation/domains/${domain.idAsString}/risk-definitions/DSRA", riskDefinitionJson)
        domain = domainDataRepository.findByIdWithProfilesAndRiskDefinitions(domainId, client.id).get()
        riskDefinition = domain.riskDefinitions.DSRA
        def c2Ref = CategoryRef.from(riskDefinition.getCategory('C2').get())

        then:
        with(riskDefinition.categories) {
            it.size() == 5
            it.find{it.id == 'C2'}.riskValuesSupported
        }

        when:
        def getProcessResponse = get("/processes/$processId")
        def processETag = getETag(getProcessResponse)
        def retrievedProcess = parseJson(getProcessResponse)

        def processDomainRiskValues = retrievedProcess.domains.get(domainId as String)
        def processDsraRiskValues = processDomainRiskValues.riskValues.DSRA

        then:
        processDsraRiskValues.potentialImpacts.keySet() ==~ ['C', 'I']

        when:
        def getProcessRiskResponse = get("/processes/$processId/risks/$scenarioId")
        def retrievedProcessRisk = parseJson(getProcessRiskResponse)

        def riskDomainRiskValues = retrievedProcessRisk.domains.get(domainId as String)
        def riskDsraRiskValues = riskDomainRiskValues.riskDefinitions.DSRA

        then:
        riskDsraRiskValues.impactValues*.category ==~ ['C', 'I', 'A', 'R', 'C2']
        riskDsraRiskValues.riskValues*.category ==~ ['C', 'I', 'A', 'R', 'C2']

        when:
        processDsraRiskValues.potentialImpacts.C2 = 1
        Map headers = [
            'If-Match': processETag
        ]

        put("/processes/$processId",retrievedProcess, headers)
        def process = processDataRepository.findWithRisksAndScenariosByIdIn([processId]).first()

        ImpactValues impactValues = process.getImpactValues(domain, rdRef).get()

        then:
        impactValues.potentialImpactsEffective[c2Ref].idRef == 1

        when:
        getProcessResponse = get("/processes/$processId")
        processETag = getETag(getProcessResponse)
        retrievedProcess = parseJson(getProcessResponse)

        processDomainRiskValues = retrievedProcess.domains.get(domainId as String)
        processDsraRiskValues = processDomainRiskValues.riskValues.DSRA

        then:
        processDsraRiskValues.potentialImpacts.keySet() ==~ ['C', 'I', 'C2']

        when:
        getProcessRiskResponse = get("/processes/$processId/risks/$scenarioId")
        def riskETag = getETag(getProcessRiskResponse)
        retrievedProcessRisk = parseJson(getProcessRiskResponse)

        riskDomainRiskValues = retrievedProcessRisk.domains.get(domainId as String)
        riskDsraRiskValues = riskDomainRiskValues.riskDefinitions.DSRA

        then:
        riskDsraRiskValues.impactValues*.category ==~  ['C', 'I', 'A', 'R', 'C2']
        riskDsraRiskValues.riskValues*.category ==~  ['C', 'I', 'A', 'R', 'C2']

        when:
        def riskDsraRiskValuesImpactC2 = riskDsraRiskValues.impactValues.find{it.category == 'C2'}
        def riskDsraRiskValuesRiskC2 = riskDsraRiskValues.riskValues.find{it.category == 'C2'}

        then:
        riskDsraRiskValuesImpactC2.specificImpact == null
        riskDsraRiskValuesRiskC2.userDefinedResidualRisk == null

        when:
        riskDsraRiskValuesImpactC2.specificImpact = 1
        riskDsraRiskValuesRiskC2.userDefinedResidualRisk = 2

        headers = [
            'If-Match': riskETag
        ]

        put("/processes/$processId/risks/$scenarioId",retrievedProcessRisk, headers)
        process = processDataRepository.findWithRisksAndScenariosByIdIn([processId]).first()
        def risk = process.risks.first() as ProcessRiskData

        def impactProvider = risk.getImpactProvider(rdRef, domain)
        def categorizedImpact = impactProvider.categorizedImpacts.find{it.category == c2Ref}

        then:
        categorizedImpact.specificImpact.idRef == 1
        categorizedImpact.effectiveImpact.idRef == 1

        when:
        def riskProvider = risk.getRiskProvider(rdRef, domain)
        def determinedRisk = riskProvider.getCategorizedRisks().find{it.category == c2Ref}

        then:
        determinedRisk.inherentRisk.idRef == 1
        determinedRisk.userDefinedResidualRisk.idRef == 2

        when:
        getProcessRiskResponse = get("/processes/$processId/risks/$scenarioId")
        retrievedProcessRisk = parseJson(getProcessRiskResponse)

        riskDomainRiskValues = retrievedProcessRisk.domains.get(domainId as String)
        riskDsraRiskValues = riskDomainRiskValues.riskDefinitions.DSRA

        then:
        riskDsraRiskValues.impactValues*.category ==~ ['C', 'I', 'A', 'R', 'C2']
        riskDsraRiskValues.riskValues*.category ==~ ['C', 'I', 'A', 'R', 'C2']
    }

    @WithUserDetails("content-creator")
    def "CRUD for DomainUpdateDefinition"() {
        given: "a domain with a changed attribute type"
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID).id
        executeInTransaction {
            def domain = domainDataRepository.findById(domainId).get()
            domainDataRepository.save(domain.tap {
                getElementTypeDefinition('scope').customAspects.scope_contactInformation.attributeDefinitions.scope_contactInformation_website = new ExternalDocumentAttributeDefinition()
            })
        }

        when:
        def md = parseJson(get("/content-creation/domains/${domainId}/migrations"))

        then: "the migrations contains one step"
        md.size() == 1

        when: "trying to explicitly set an empty migration list"
        parseJson(put("/content-creation/domains/${domainId}/migrations", [], [:], 422))

        then:
        def updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Missing migration steps: Modified attribute 'scope_contactInformation_website' of custom aspect 'scope_contactInformation' for type scope"

        when: "we update the migrations"
        parseJson(put("/content-creation/domains/${domainId}/migrations", migrationDefinition(),[:], 200))

        def results = get("/content-creation/domains/${domainId}/migrations")
        String eTag = getETag(results)
        md = parseJson(results)

        then: "the update is persistent"
        with(md.first()) {
            description.en == "a type change"
            oldDefinitions.size() == 1
            newDefinitions.size() == 1
        }

        and: "another get returns 304"
        mvc.perform(MockMvcRequestBuilders.get("/content-creation/domains/${domainId}/migrations").accept(MediaType.APPLICATION_JSON).header(
                HttpHeaders.IF_NONE_MATCH, eTag
                )).andReturn().response.status == HttpStatus.SC_NOT_MODIFIED

        when: "the customAspect does not exist"
        migrationDefinition().tap {
            first().oldDefinitions.first().customAspect = "no_existing_aspect"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Invalid oldDefinition 'a1'. No customAspect 'no_existing_aspect' for element type scope."

        when: "the attribute does not exist"
        migrationDefinition().tap {
            first().oldDefinitions.first().attribute = "no_existing_attribute"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Invalid oldDefinition 'a1'. No attribute 'scope_contactInformation.no_existing_attribute' for element type scope."

        when: "no description is provided"
        migrationDefinition().tap {
            first().description = [:]
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: No description provided for step 'a1'."

        when: "the migrationExpression is wrong"
        migrationDefinition().tap {
            first().newDefinitions.first().migrationExpression.customAspect = "no_existing"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Invalid newDefinition 'a1'. MigrationExpression is invalid: Custom aspect 'no_existing' is not defined."

        when: "the element type is wrong"
        migrationDefinition().tap {
            first().newDefinitions.first().elementType = "none_existing_element_type"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Invalid newDefinition 'a1'. 'none_existing_element_type' is not a valid element type - must be one of asset, control, document, incident, person, process, scenario, scope"

        when: "the customAspect does not exist"
        migrationDefinition().tap {
            first().newDefinitions.first().customAspect = "no_existing"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Invalid newDefinition 'a1'. No customAspect 'no_existing' for element type scope."

        when: "the attribute in the migrationExpression does not exist"
        migrationDefinition().tap {
            first().newDefinitions.first().migrationExpression.attribute = "no_existing_attribute"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Invalid newDefinition 'a1'. MigrationExpression is invalid: Attribute 'no_existing_attribute' is not defined."

        when: "the steps have the same id"
        migrationDefinition().tap {
            it.add(migrationDefinition().first())
            put("/content-creation/domains/${domainId}/migrations", it, [:], 422)
        }

        then:"the data is rejected"
        updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 2.0.0: Id 'a1' not unique."

        when: "the steps have different ids"
        migrationDefinition().tap {
            it.add(migrationDefinition().first())
            first().id = "a2"
            put("/content-creation/domains/${domainId}/migrations", it, [:], 200)
        }

        then: "the update is persistent"
        with(parseJson(get("/content-creation/domains/${domainId}/migrations"))) {
            size() == 2
            it.get(0).id == "a2"
            it.get(1).id == "a1"
        }

        when: "we change another CA definition"
        executeInTransaction {
            def domain = domainDataRepository.findById(domainId).get()
            domainDataRepository.save(domain.tap {
                getElementTypeDefinition('process').customAspects.get('process_opinionDPO').tap {
                    attributeDefinitions.remove('process_opinionDPO_opinionDPO')
                    attributeDefinitions.put('process_opinionDPO_new', new TextAttributeDefinition())
                }
            })
        }

        then:
        put("/content-creation/domains/${domainId}/migrations", migrationDefinition().plus(migrationDefinitionChangeKey()),[:], 200)
    }

    @WithUserDetails("content-creator")
    def "domains with no previous major and no breaking changes must not contain migrations"() {
        given: "an unmodified domain"
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID).id

        when:
        parseJson(put("/content-creation/domains/$domainId/migrations", migrationDefinition(), [:], 422))

        then:
        def ex = thrown(UnprocessableDataException)
        ex.message == "Migrations must be empty, because no breaking changes from domain template 1.4.0 were detected and no previous major version template (0.*.*) was found."
    }

    @WithUserDetails("content-creator")
    def "domain with no breaking changes must contain migration steps from previous major"() {
        given: "an unmodified domain based on 2.0.0"
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID).id

        when: "trying to use a definition that didn't exist in the old template"
        parseJson(put("/content-creation/domains/${domainId}/migrations", [
            [description : [en: "Go figure it out"],
                id: "a",
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "document",
                        customAspect: "document_details",
                        attribute: "document_details_status"
                    ],
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "document",
                        customAspect: "document_details",
                        attribute: "document_details_status",
                        migrationExpression: [
                            type : 'constant',
                            value: 'good'
                        ]
                    ],
                ]
            ],
        ], [:], 422))

        then:
        def updateEx = thrown(UnprocessableDataException)
        updateEx.message == "Migration definition not suited to update from old domain template 1.4.0: Invalid oldDefinition 'a'. No attribute 'document_details.document_details_status' for element type document."

        when: "trying to explicitly set an empty migration list"
        parseJson(put("/content-creation/domains/${domainId}/migrations", [], [:], 422))

        then:
        updateEx = thrown(UnprocessableDataException)
        updateEx.message.startsWith("Migration definition not suited to update from old domain template 1.4.0: Missing migration steps: ")
        updateEx.message.contains("Removed attribute 'process_PIADPO_advice' of custom aspect 'process_PIADPO' for type process")
        updateEx.message.contains("Removed attribute 'process_PIAInvolvement_affectedPersonsComment' of custom aspect 'process_PIAInvolvement' for type process")
    }

    @WithUserDetails("content-creator")
    def "Migrations must be empty on a domain not based on a template"() {
        given: "an unmodified domain"
        def domainId = parseJson(post("/content-creation/domains", [
            name: "migration validation test domain",
            authority: "yours truly",
        ])).resourceId

        when:
        parseJson(put("/content-creation/domains/$domainId/migrations", migrationDefinition(), [:], 422))

        then:
        def ex = thrown(UnprocessableDataException)
        ex.message == "Migrations must be empty, because the domain is not based on a template."
    }

    private List<Map> migrationDefinitionChangeKey() {
        def m = [
            [description : [en: "a key change"],
                id: "b1",
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "process",
                        customAspect: "process_opinionDPO",
                        attribute: "process_opinionDPO_opinionDPO"
                    ],
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "process",
                        customAspect: "process_opinionDPO",
                        attribute: "process_opinionDPO_new",
                        migrationExpression: [
                            type : 'customAspectAttributeValue',
                            customAspect: 'process_opinionDPO',
                            attribute: 'process_opinionDPO_opinionDPO'
                        ]
                    ],
                ]
            ],
        ]
        return m
    }

    private List<Map> migrationDefinition() {
        def m = [
            [description : [en: "a type change"],
                id: "a1",
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "scope",
                        customAspect: "scope_contactInformation",
                        attribute: "scope_contactInformation_website"
                    ],
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "scope",
                        customAspect: "scope_contactInformation",
                        attribute: "scope_contactInformation_website",
                        migrationExpression: [
                            type : 'customAspectAttributeValue',
                            customAspect: 'scope_contactInformation',
                            attribute: 'scope_contactInformation_website'
                        ]
                    ],
                ]
            ],
        ]
        return m
    }
}