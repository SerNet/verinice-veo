/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.rest

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Control
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned.Lifecycle
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.ControlGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DocumentRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ControlData
import org.veo.persistence.entity.jpa.groups.AssetGroupData
import org.veo.persistence.entity.jpa.groups.ControlGroupData
import org.veo.persistence.entity.jpa.groups.DocumentGroupData
import org.veo.persistence.entity.jpa.groups.ProcessGroupData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)

@ComponentScan("org.veo.rest")
class GroupControllerMockMvcITSpec extends VeoRestMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private DocumentRepositoryImpl documentRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Unit unit2
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    @Autowired
    private EntityDataFactory entityFactory

    def setup() {
        txTemplate.execute {
            domain = entityFactory.createDomain()
            domain.description = "ISO/IEC"
            domain.abbreviation = "ISO"
            domain.name = "ISO"
            domain.id = Key.newUuid()

            domain1 = entityFactory.createDomain()
            domain1.description = "ISO/IEC2"
            domain1.abbreviation = "ISO"
            domain1.name = "ISO"
            domain1.id = Key.newUuid()

            def client= entityFactory.createClient()
            client.id = clientId
            client.domains = [domain, domain1] as Set

            unit = entityFactory.createUnit()
            unit.name = "Test unit"
            unit.id = Key.newUuid()
            unit.client = client

            unit2 = entityFactory.createUnit()
            unit2.name = "Test unit"
            unit2.id = Key.newUuid()
            unit2.client = client

            clientRepository.save(client)
            unitRepository.save(unit)
            unitRepository.save(unit2)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create an asset group"() {
        given: "a request body"

        Map request = [
            name: 'My Assets',
            type: 'Asset',
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
            ]
        ]

        when: "the request is sent"

        def results = post('/groups', request)

        then: "the asset group is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new asset group is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Group created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "create a process group"() {
        given: "a request body"

        Map request = [
            name: 'My Processes',
            type: 'Process',
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
            ]
        ]

        when: "the request is sent"

        def results = post('/groups', request)

        then: "the process group is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new asset group is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Group created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a document group"() {
        given: "a saved document group"
        DocumentGroup documentGroup = new DocumentGroupData().with{
            id = Key.newUuid()
            name = 'Test document group'
            owner = unit
            state = Lifecycle.CREATING
            it
        }

        documentGroup = txTemplate.execute {
            documentRepository.save(documentGroup)
        }

        when: "the server is queried for the group"
        def results = get("/groups/${documentGroup.id.uuidValue()}?type=Document")

        then: "the document group is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test document group'
        result.owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a control group with members"() {
        given: "a saved control group with two members"

        Control c1 = new ControlData()
        c1.id = Key.newUuid()
        c1.name = "c1"
        c1.owner = unit

        Control c2 = new ControlData()
        c2.id = Key.newUuid()
        c2.name = "c2"
        c2.owner = unit

        ControlGroup controlGroup = new ControlGroupData().with {
            id = Key.newUuid()
            name = 'Test control group'
            owner = unit
            members = [c1, c2]
            state = Lifecycle.CREATING
            it
        }
        controlGroup = txTemplate.execute {
            controlRepository.save(controlGroup)
        }

        when: "the server is queried for the group"
        def results = get("/groups/${controlGroup.id.uuidValue()}?type=Control")

        then: "the control group is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test control group'
        result.owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"
        result.members.size() == 2
        result.members*.displayName as Set == ['c1', 'c2'] as Set
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a control group's members"() {
        given: "a saved control group with two members"

        Control c1 = new ControlData()
        c1.id = Key.newUuid()
        c1.name = "c1"
        c1.owner = unit

        Control c2 = new ControlData()
        c2.id = Key.newUuid()
        c2.name = "c2"
        c2.owner = unit

        ControlGroup controlGroup = new ControlGroupData().with {
            id = Key.newUuid()
            name = 'Test control group'
            owner = unit
            members = [c1, c2]
            state = Lifecycle.CREATING
            it
        }
        controlGroup = txTemplate.execute {
            controlRepository.save(controlGroup)
        }

        when: "the server is queried for the group's members"
        def results = get("/groups/${controlGroup.id.uuidValue()}/members?type=Control")

        then: "the control group is found"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2
        result.sort{it.name}.first().name == 'c1'
        result.sort{it.name}[1].name == 'c2'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all process groups for a client"() {
        given: "two saved process grousp"
        ProcessGroup processGroup1 = new ProcessGroupData().with{
            id = Key.newUuid()
            name = 'Test process group 1'
            owner = unit
            state = Lifecycle.CREATING
            it
        }
        ProcessGroup processGroup2 = new ProcessGroupData().with{
            id = Key.newUuid()
            name = 'Test process group 2'
            state = Lifecycle.CREATING
            owner = unit
            it
        }

        (processGroup1, processGroup2) =
                txTemplate.execute {
                    [processGroup1, processGroup2].collect(processRepository.&save)
                }

        when: "the server is queried for the process groups"
        def results = get("/groups?type=Process")

        then: "the groups are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test process group 1'
        result.sort{it.name}.first().owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"

        result.sort{it.name}[1].name == 'Test process group 2'
        result.sort{it.name}[1].owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all asset groups for a unit"() {
        given: "a saved asset"


        def assetGroup1 = new AssetGroupData().with {
            id = Key.newUuid()
            name = 'Test asset group 1'
            owner = unit
            state = Lifecycle.CREATING
            it
        }

        def assetGroup2 = new AssetGroupData().with {
            id = Key.newUuid()
            name = 'Test asset group 2'
            owner = unit2
            state = Lifecycle.CREATING
            it
        }

        (assetGroup1, assetGroup2) =
                txTemplate.execute {
                    [assetGroup1, assetGroup2].collect(assetRepository.&save)
                }
        when: "a request is made to the server for all assets groups of a unit"
        def results = get("/groups?unit=${unit.id.uuidValue()}&type=Asset")

        then: "the respective asset group is returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset group 1'
        result.first().owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"

        when: "a request is made to the server for all assets groups of another unit"
        results = get("/groups?unit=${unit2.id.uuidValue()}&type=Asset")

        then: "the respective asset group is returned"
        results.andExpect(status().isOk())
        when:
        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset group 2'
        result.first().owner.targetUri == "http://localhost/units/${unit2.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieving groups works if there are also non-group entities"() {
        given: "a control and a control group"
        Control control = entityFactory.createControl(Key.newUuid(), "c1", unit)

        ControlGroup controlGroup = new ControlGroupData().tap{
            id = Key.newUuid()
            name = 'Group 1'
            owner = unit
        }

        txTemplate.execute {
            [control, controlGroup].each(controlRepository.&save)
        }

        when: "the server is queried for the control groups"
        def results = get("/groups?type=Control")

        then: "the groups are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1
        result.first().name == controlGroup.name
    }

    @WithUserDetails("user@domain.example")
    def "put an asset group with custom properties"() {
        given: "a saved asset group"

        CustomProperties cp = entityFactory.createCustomProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Asset'] as Set)

        Key<UUID> id = Key.newUuid()
        AssetGroup assetGroup = new AssetGroupData().with {
            setId(id)
            name = 'Test asset group'
            setOwner(unit)
            customAspects = [cp]
            domains = [domain1]
            state = Lifecycle.CREATING
            it
        }

        assetGroup = txTemplate.execute {
            assetRepository.save(assetGroup)
        }

        Map request = [
            name: 'New asset group-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: "/units/${unit.id.uuidValue()}",
                displayName: 'test unit'
            ], domains: [
                [
                    targetUri: "/domains/${domain.id.uuidValue()}",
                    displayName: 'test ddd'
                ]
            ], customAspects:
            [
                'my.aspect-test' :
                [
                    type : 'my.aspect-test1',
                    applicableTo: [
                        "Asset"
                    ],
                    domains: [],
                    attributes:  [
                        test1:'value1',
                        test2:'value2'
                    ]
                ]
            ]
        ]

        when: "the new group data is sent to the server"
        def results = put("/groups/${id.uuidValue()}?type=Asset",request)

        then: "the group is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New asset group-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == "${domain.abbreviation} ${domain.name}"
        result.owner.targetUri == "http://localhost/units/${unit.id.uuidValue()}"

        when:
        def entity = txTemplate.execute {
            assetRepository.findById(id).get().tap() {
                // resolve proxy:
                customAspects.first()
            }
        }

        then:
        entity.name == 'New asset group-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'my.aspect-test1'
        entity.customAspects.first().applicableTo == ['Asset'] as Set
        entity.customAspects.first().stringProperties.test1 == 'value1'
        entity.customAspects.first().stringProperties.test2 == 'value2'
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset group"() {

        given: "an existing asset group"
        AssetGroup assetGroup = new AssetGroupData().with {
            setId(Key.newUuid())
            name = 'Test asset-delete'
            owner = unit
            setDomains([domain1] as Set)
            state = Lifecycle.CREATING
            it
        }

        assetGroup = txTemplate.execute {
            assetRepository.save(assetGroup)
        }

        when: "a delete request is sent to the server"

        def results = delete("/groups/${assetGroup.id.uuidValue()}?type=Asset")

        then: "the asset group is deleted"
        results.andExpect(status().isOk())
        assetRepository.findById(assetGroup.id).empty
    }

    @WithUserDetails("user@domain.example")
    def "can't put a group with another group's ID"() {
        given: "two groups"
        def group1 = txTemplate.execute({
            assetRepository.save(new AssetGroupData().tap {
                id = Key.newUuid()
                owner = unit
                name = "old name 1"
            })
        })
        def group2 = txTemplate.execute({
            assetRepository.save(new AssetGroupData().tap {
                id = Key.newUuid()
                owner = unit
                name = "old name 2"
            })
        })
        when: "a put request tries to update group 1 using the ID of group 2"
        put("/groups/${group2.id.uuidValue()}?type=Asset", [
            id: group1.id.uuidValue(),
            name: "new name 1",
            owner: [targetUri: '/units/' + unit.id.uuidValue()]
        ], false)
        then: "an exception is thrown"
        thrown(DeviatingIdException)
    }
}