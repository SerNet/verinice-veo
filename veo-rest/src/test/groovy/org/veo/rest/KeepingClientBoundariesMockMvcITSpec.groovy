/*******************************************************************************
 * Copyright (c) 2020 Daniel Murygin.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.EntityTypeNames
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

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
@EnableAsync
@ComponentScan("org.veo.rest")
class KeepingClientBoundariesMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    TransactionTemplate txTemplate

    private Client client
    private Client otherClient
    private Unit unit
    private Unit otherClientsUnit
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

    def setup() {
        txTemplate.execute {

            client = newClient {
                name = "Test Client"
            }
            client.id = clientId

            unit = newUnit(client) {
                name = "Test unit"
            }
            unit.id = Key.newUuid()

            clientRepository.save(client)
            unitRepository.save(unit)

            otherClient = newClient()
            clientRepository.save(otherClient)

            otherClientsUnit = newUnit(otherClient)
            unitRepository.save(otherClientsUnit)
        }
        ETag.setSalt(salt)
    }

    @WithUserDetails("user@domain.example")
    def "can't create an asset in another client"() {
        when: "a post request tries to create an asset in another client"
        postEntityInAnotherUnit("/" + EntityTypeNames.ASSETS,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't create a control in another client"() {
        when: "a post request tries to create a control in another client"
        postEntityInAnotherUnit("/" + EntityTypeNames.CONTROLS,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't create a person in another client"() {
        when: "a post request tries to create a person in another client"
        postEntityInAnotherUnit("/" + EntityTypeNames.PERSONS,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't create a process in another client"() {
        when: "a post request tries to create a process in another client"
        postEntityInAnotherUnit("/" + EntityTypeNames.PROCESSES,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't change the client of a unit"() {
        given: "a unit that belongs to another client"
        def otherClientsUnit = txTemplate.execute({
            Unit unit = newUnit(otherClient)
            unit.setParent(otherClientsUnit)
            unitRepository.save(unit)
        })
        when: "a put request tries to move the asset to the user's unit"
        Map headers = [
            'If-Match': ETag.from(otherClientsUnit.id.uuidValue(), 0)
        ]
        put("/" + EntityTypeNames.UNITS + "/" + otherClientsUnit.id.uuidValue() , [
            id: '' + otherClientsUnit.id.uuidValue(),
            name: 'hijacked-unit',
            parent: [displayName: 'Test unit',
                targetUri: 'http://localhost//units/' + unit.id.value()]
        ], headers, false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't change the client of an asset"() {
        given: "an asset that belongs to another client"
        def otherClientsAsset = txTemplate.execute({
            assetRepository.save(newAsset(otherClientsUnit))
        })
        when: "a put request tries to move the asset to the user's unit"
        putEntityToAnotherUnit("/" + EntityTypeNames.ASSETS, otherClientsAsset.getId().uuidValue())
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't change the client of a control"() {
        given: "a control that belongs to another client"
        def otherClientsControl = txTemplate.execute({
            controlRepository.save(newControl(otherClientsUnit))
        })
        when: "a put request tries to move the control to the user's unit"
        putEntityToAnotherUnit("/" + EntityTypeNames.CONTROLS, otherClientsControl.getId().uuidValue())
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't change the client of a person"() {
        given: "a person that belongs to another client"
        def otherClientsPerson = txTemplate.execute({
            personRepository.save(newPerson(otherClientsUnit))
        })
        when: "a put request tries to move the person to the user's unit"
        putEntityToAnotherUnit("/" + EntityTypeNames.PERSONS, otherClientsPerson.getId().uuidValue())
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't change the client of a process"() {
        given: "a process that belongs to another client"
        def otherClientsProcess = txTemplate.execute({
            processRepository.save(newProcess(otherClientsUnit))
        })
        when: "a put request tries to move the process to the user's unit"
        putEntityToAnotherUnit("/" + EntityTypeNames.PROCESSES, otherClientsProcess.getId().uuidValue())
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't delete a unit from another client"() {
        given: "a unit that belongs to another client"
        def otherClientsUnit = txTemplate.execute({
            unitRepository.save(newUnit(otherClient))
        })
        when: "a delete request tries to delete the unit"
        delete("/" + EntityTypeNames.UNITS + "/" + otherClientsUnit.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't delete an asset from another client"() {
        given: "an asset that belongs to another client"
        def otherClientsAsset = txTemplate.execute({
            assetRepository.save(newAsset(otherClientsUnit))
        })
        when: "a delete request tries to delete the asset"
        delete("/" + EntityTypeNames.ASSETS + "/" + otherClientsAsset.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't delete a control from another client"() {
        given: "a control that belongs to another client"
        def otherClientsControl = txTemplate.execute({
            controlRepository.save(newControl(otherClientsUnit))
        })
        when: "a delete request tries to delete the control"
        delete("/" + EntityTypeNames.CONTROLS + "/" + otherClientsControl.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't delete a person from another client"() {
        given: "a person that belongs to another client"
        def otherClientsPerson = txTemplate.execute({
            personRepository.save(newPerson(otherClientsUnit))
        })
        when: "a delete request tries to delete the person"
        delete("/" + EntityTypeNames.PERSONS + "/" + otherClientsPerson.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't delete a process from another client"() {
        given: "a process that belongs to another client"
        def otherClientsProcess = txTemplate.execute({
            processRepository.save(newProcess(otherClientsUnit))
        })
        when: "a delete request tries to delete the process"
        delete("/" + EntityTypeNames.PROCESSES + "/" + otherClientsProcess.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't get an asset of another client"() {
        given: "an asset that belongs to another client"
        def otherClientsAsset = txTemplate.execute({
            assetRepository.save(newAsset(otherClientsUnit))
        })
        when: "a get request tries to get the asset"
        get("/" + EntityTypeNames.ASSETS + "/" + otherClientsAsset.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't get a control of another client"() {
        given: "a control that belongs to another client"
        def otherClientsControl = txTemplate.execute({
            controlRepository.save(newControl(otherClientsUnit))
        })
        when: "aa get request tries to get the control"
        get("/" + EntityTypeNames.CONTROLS + "/" + otherClientsControl.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't get a person of another client"() {
        given: "a person that belongs to another client"
        def otherClientsPerson = txTemplate.execute({
            personRepository.save(newPerson(otherClientsUnit))
        })
        when: "a get request tries to get the person"
        get("/" + EntityTypeNames.PERSONS + "/" + otherClientsPerson.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't get a process of another client"() {
        given: "a process that belongs to another client"
        def otherClientsProcess = txTemplate.execute({
            processRepository.save(newProcess(otherClientsUnit))
        })
        when: "a get request tries to get the process"
        get("/" + EntityTypeNames.PROCESSES + "/" + otherClientsProcess.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't get a unit of another client"() {
        given: "a unit that belongs to another client"
        def otherClientsUnit = txTemplate.execute({
            Unit unit = newUnit(otherClient)
            unit.setParent(otherClientsUnit)
            unitRepository.save(unit)
        })
        when: "a get request tries to get the unit"
        get("/" + EntityTypeNames.UNITS + "/" + otherClientsUnit.getId().uuidValue(), false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't link to an asset of another client"() {
        given: "an asset that belongs to another client"
        def otherClientsAsset = txTemplate.execute({
            assetRepository.save(newAsset(otherClientsUnit))
        })
        when: "a post request tries to link a process to the asset of the other client"
        post('/processes', [
            name : 'My process',
            owner: [
                targetUri: 'http://localhost/units/units/'+unit.id.uuidValue()
            ],
            links: [
                'Process_depends_on_Asset': [
                    [
                        name  : 'requires',
                        target:
                        [
                            targetUri: "http://localhost/units/assets/$otherClientsAsset.dbId"
                        ]
                    ]
                ]
            ]
        ], false)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    def postEntityInAnotherUnit(String url) {
        post(url, [
            name: 'entity-in-another client',
            owner: [displayName: '' + otherClientsUnit.name,
                targetUri: 'http://localhost/units/' + otherClientsUnit.id.value()]
        ],false)
    }

    def putEntityToAnotherUnit(String url, String entityUuid) {
        Map headers = [
            'If-Match': ETag.from(entityUuid, 0)
        ]
        put("${url}/${entityUuid}", [
            id: '' + entityUuid,
            name: 'hijacked-entity',
            owner: [displayName: 'Test unit',
                targetUri: 'http://localhost/units/' + unit.id.value()]
        ], headers, false)
    }
}
