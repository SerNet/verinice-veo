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

import java.nio.charset.StandardCharsets

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.method.annotation.HandlerMethodValidationException

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.exception.EntityAlreadyExistsException
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.usecase.domain.DomainInUseException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository

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
            }
        }

        when: "exporting with compact media type"
        export = parseJson(get("/content-creation/domain-templates/$TEST_DOMAIN_TEMPLATE_ID", 200, CompactJsonHttpMessageConverter.MEDIA_TYPE_JSON_COMPACT))

        then: "default and read-only values are absent"
        export.name != null
        !export.containsKey("profiles_v2")
        with(export.catalogItems.find { it.abbreviation == "cc-1" }) {
            !it.containsKey("customAspects")
            with(tailoringReferences.first()) {
                target.targetUri != null
                !target.containsKey("id")
                !target.containsKey("name")
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
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }

        when: "a request is made to the server"
        def result = post("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope/object-schema", schemaJson, 204)

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
        with(updatedDomain.getElementTypeDefinition('scope')) {
            with(it.subTypes) {
                it.keySet() == [
                    'SCP_Scope',
                    'SCP_Processor',
                    'SCP_Controller',
                    'SCP_JointController',
                    'SCP_ResponsibleBody'
                ] as Set
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
        post("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope/object-schema", schemaJson, 400)

        then:
        thrown(IllegalArgumentException)
    }

    @WithUserDetails("content-creator")
    def "update an element type definition in a domain"() {
        given:
        def schemaJson = [
            subTypes:[
                SCP_Container:[
                    statuses:['Empty', 'Full']
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
                    container_lid_present: 'Lid present?',
                    container_owner: 'Owner'
                ]
            ],
        ]

        when: "updating the scope definition"
        put("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope", schemaJson, 204)

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
        with(updatedDomain.getElementTypeDefinition('scope')) {
            with(it.subTypes) {
                it.keySet() ==~ [
                    'SCP_Container'
                ]
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
        def status = postUnauthorized("/content-creation/domains/${testDomain.id.uuidValue()}/elementtypedefinitions/scope/object-schema", schemaJson)

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type definition as regular user"() {
        when:
        def response = putUnauthorized("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope", [:])

        then:
        response.andReturn().response.status == 403
    }

    @WithUserDetails("content-creator")
    def "create a DomainTemplate"() {
        given: "a number of existing templates"
        def initialTemplateCount = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        when: "a template is created"
        def result = parseJson(post("/content-creation/domains/${testDomain.id.uuidValue()}/template",[version : "1.0.0"]))

        then: "a result is returned"
        result != null

        and: "there is one more template in the repo"
        domainTemplateDataRepository.count() == initialTemplateCount + 1

        when: "loading the domain templates from the database"
        def dt = txTemplate.execute {
            domainTemplateRepository.findAll().find{ it.name == "Domain 1" }
        }

        then: "the version is set"
        dt.templateVersion == "1.0.0"

        when: "trying to create another domain template with the same version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.0"], 409)

        then:
        thrown(EntityAlreadyExistsException)

        when: "trying to create another domain template with a lower version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "0.5.3"], 422)

        then:
        thrown(UnprocessableDataException)

        when: "trying to create another domain template with an invalid version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.1"], 400)

        then:
        thrown(HandlerMethodValidationException)

        when: "trying to create another domain template with a prerelease label"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.1-prerelease3"], 400)

        then:
        thrown(HandlerMethodValidationException)

        when: "trying to create another domain template with a higher version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.1"])

        then:
        notThrown(Exception)
    }

    @WithUserDetails("content-creator")
    def "create catalog items in a domain from a unit"() {
        given: "a domain and a unit with elements"
        Domain domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def domainId = domain.idAsString
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId)

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

        def catalogItems = txTemplate.execute {
            domainDataRepository.findById(domainId).get().catalogItems.each {
                it.tailoringReferences.size()
            }
        }

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
        def incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${catalogItemsId}"))
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
        incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${catalogItemsIds}"))
        elementList = parseJson(post("/units/${unitId}/incarnations", incarnationDescription))

        assetId = elementList.find {it.targetUri.contains('assets')}.targetUri.split('/' ).last()
        def asset = txTemplate.execute {
            assetDataRepository.findById(assetId).get()
        }

        def scenarioIds = elementList.collect {it.targetUri}.findAll {it.contains('scenarios')}.collect {it.split('/' ).last()}
        def scenarios = txTemplate.execute {
            def list = scenarioDataRepository.findAllWithCompositesAndCompositesPartsByDbIdIn(scenarioIds)
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
            assetDataRepository.findById(assetId).get()
        }

        then: "the reference to the updated catalog item is intact"
        asset.appliedCatalogItems.size() == 1

        when: "we incarnate scenario 1 and link the composite feature to the first scenario"
        catalogItems = txTemplate.execute {
            domainDataRepository.findById(domainId).get().catalogItems.each {
                it.tailoringReferences.size()
            }
        }

        def scenarioItemIds = catalogItems.find{ it.name =="example scenario 1" }.collect {it.symbolicIdAsString}.join(',')
        incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${scenarioItemIds}&mode=MANUAL"))
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
            scenarioDataRepository.findById(existingScenarioInNewUnitId).get().tap {
                parts.collect { it.parts.size() }
                composites.collect { it.composites.size() }
            }
        }
        def newScenario1 = txTemplate.execute {
            def q = scenarioDataRepository.findById(elementList[0].targetUri.split('/' ).last()).get()
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
        incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${scenarioItemIds}&mode=MANUAL&include=PART"))
        references = incarnationDescription.parameters.references.first()

        then: "only PART is returned"
        incarnationDescription.parameters.size() == 1
        references.size() == 1
        references.referenceType == ["PART"]

        when: "we exclude PART"
        incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${scenarioItemIds}&mode=MANUAL&exclude=PART"))
        references = incarnationDescription.parameters.references.first()

        then: "only COMPOSITE is returned"
        incarnationDescription.parameters.size() == 1
        references.size() == 1
        references.referenceType == ["COMPOSITE"]
    }

    @WithUserDetails("content-creator")
    def "create an empty profile in a domain"() {
        given: "a domain"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString

        when: "we create a new empty profile"
        post("/content-creation/domains/${domainId}/profiles",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE'
                ], 201)

        Domain domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.size()
            d
        }

        then: "the profile is created"
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test"
            description == "All the good stuff"
            language == "de_DE"
        }

        when: "we update the empty profile"
        def profileId = domain1.getProfiles().first().idAsString
        put("/content-creation/domains/${domainId}/profiles/${profileId}",
                [
                    name: 'test1',
                    description: 'All the good stuff, but better.',
                    language: 'de_DE'
                ], 204)

        domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.size()
            d
        }

        then: "the profile is updated"
        domain1.getProfiles().size()==1
        with(domain1.getProfiles().first()) {
            name == "test1"
            description == "All the good stuff, but better."
            language == "de_DE"
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
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        def unitId = createUnitWithElements(domainId).first()

        when: "we create a new profile"
        post("/content-creation/domains/${domainId}/profiles?unit=${unitId}",
                [
                    name: 'test',
                    description: 'All the good stuff',
                    language: 'de_DE'
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
            items.size() == 8
        }

        when: "we update the profile meta data"
        def profileId = domain1.getProfiles().first().idAsString
        put("/content-creation/domains/${domainId}/profiles/${profileId}",
                [
                    name: 'test1',
                    description: 'All the good stuff, but better.',
                    language: 'de_DE'
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
                    language: 'de_DE'
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
            items.size() == 9
        }
    }

    @WithUserDetails("content-creator")
    def "export a profile from a domain"() {
        given: "a domain and a unit"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId, true)

        when: "we incarnate one linked catalog item"
        def catalogItems = txTemplate.execute {
            domainDataRepository.findById(domainId).get().catalogItems.each {
                it.tailoringReferences.size()
            }
        }
        def catalogItemsId = catalogItems.find{it.name == "Control-2" }.symbolicIdAsString

        def incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${catalogItemsId}"))
        post("/units/${unitId}/incarnations", incarnationDescription)

        and: "we link the process with the asset"
        post("/domains/$domainId/processes/$processId/links", [
            process_requiredApplications: [
                [
                    target: [targetUri: "/assets/$assetId"]
                ]
            ]
        ], 204)

        and: "we create a new profile and export"
        def profileId = parseJson(post("/content-creation/domains/${domainId}/profiles?unit=${unitId}",
                [
                    name: 'export-test',
                    description: 'All the good stuff',
                    language: 'de_DE'
                ], 201)).id

        def exportedProfile = parseJson(get("/domains/${domainId}/profiles/${profileId}/export"))

        then: "the profile is exported"
        with(exportedProfile) {
            name == 'export-test'
            description == 'All the good stuff'
            items.size() == 9
        }
        with(exportedProfile.items.find{it.name == "Control-2" }) {
            abbreviation == 'c-2'
            appliedCatalogItem.name == 'Control-2'
        }
        with(exportedProfile.items.find{it.name == "process" }) {
            tailoringReferences.size() == 2
            tailoringReferences.referenceType ==~  ['LINK', 'RISK']
        }
    }

    @WithUserDetails("content-creator")
    def "create profile in a domain from a unit"() {
        given: "a domain and a unit with elements"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
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
                    language: 'de_DE'
                ], 201))

        then: "the resource is returned"
        with(profileRef) {
            displayName == "test"
            targetUri.contains("domains/${domainId}/profiles/")
        }

        when: "we select the data in the db"
        def domain1 = txTemplate.execute {
            def d = domainDataRepository.findById(domainId).get()
            d.profiles.each {
                it.items.size()
                it.items.each{
                    it.tailoringReferences.size()
                }
            }
            d
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
        def profiles = parseJson(get("/domains/${domain1.id.uuidValue()}/profiles"))

        then: "the profile is returned"
        profiles.size() == 1
        with(profiles[0]) {
            name == 'test'
            description == 'All the good stuff'
            language == 'de_DE'
        }

        when: "we get the profile items"
        def profileItems = parseJson(get("/domains/${domain1.id.uuidValue()}/profiles/${profiles[0].id}/items"))

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
    def "create a profile in a domain from a unit with applied items "() {
        given: "a domain and a unit with elements"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
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
        def incarnationDescription = parseJson(get("/units/${unitId}/incarnations?itemIds=${catalogItemsIds}"))
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
            tailoringReferences.size() == 2
            with(tailoringReferences.sort {it.referenceType}) {
                it[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
                it[0].linkType == "process_requiredApplications"
                it[1].referenceType == RISK
                it[1].mitigation.name == "control"
                it[1].riskOwner.name == "person"
                it[1].target.name == "scenario"
            }
        }
    }

    @WithUserDetails("content-creator")
    def "risks are not supported for catalog items"() {
        given: "a unit with risks"
        def domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
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
        def unitId = createUnitWithElements(domain.idAsString, true).first()

        given: "a number of existing templates"
        def initialTemplateCount = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        when: "a profile and template are created"
        post("/content-creation/domains/$domain.idAsString/profiles?unit=$unitId", [
            name: 'Example elements',
            description: 'All the good stuff',
            language: 'de_DE',
        ])
        def result = parseJson(post("/content-creation/domains/${domain.id.uuidValue()}/template", [
            version : "1.2.3"
        ]))

        then: "a result is returned"
        result != null

        and: "there is one more template in the repo"
        domainTemplateDataRepository.count() == initialTemplateCount + 1

        when: "loading the domain templates from the database"
        def dt = txTemplate.execute {
            domainTemplateRepository.findAll()
                    .find{ it.name == domain.name && it.templateVersion == "1.2.3"}
                    .tap{
                        it.profiles*.items*.tailoringReferences*.id
                    } // init proxy
        }

        then: "the template is found, the version is set"
        dt.templateVersion == "1.2.3"

        and: "the example profile exists"
        with(dt.profiles.find { it.name == "Example elements" }) {
            description == 'All the good stuff'
            language == 'de_DE'

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
        Domain newDomain = createTestDomain(client, dt.id.uuidValue())
        def results = get("/domains/${newDomain.id.uuidValue()}/export")
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
        newDomain = createTestDomain(client, domainTemplateId)

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
    def "Profile metadata are optional"() {
        given:
        Domain domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def unitId = createUnitWithElements(domain.idAsString).first()

        when: "a template is created"
        post("/content-creation/domains/${domain.id.uuidValue()}/template", [
            version : "1.2.3",
            profiles: [
                exampleOrganization: [
                    unitId: unitId
                ]
            ]
        ])

        then: "a result is returned"
        noExceptionThrown()
    }

    @WithUserDetails("user@domain.example")
    def "create a domain template forbidden for user"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def status = postUnauthorized("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.0"])

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }
}