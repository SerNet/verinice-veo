/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Daniel Murygin.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
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

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)

            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })

            otherClient = clientRepository.save(newClient())
            otherClientsUnit = unitRepository.save(newUnit(otherClient))
        }
    }

    @WithUserDetails("user@domain.example")
    def "can't create an asset in another client"() {
        when: "a post request tries to create an asset in another client"
        postEntityInAnotherUnit("/" + Asset.PLURAL_TERM,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't create a control in another client"() {
        when: "a post request tries to create a control in another client"
        postEntityInAnotherUnit("/" + Control.PLURAL_TERM,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't create a person in another client"() {
        when: "a post request tries to create a person in another client"
        postEntityInAnotherUnit("/" + Person.PLURAL_TERM,)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "can't create a process in another client"() {
        when: "a post request tries to create a process in another client"
        postEntityInAnotherUnit("/" + Process.PLURAL_TERM,)
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
        put("/" + Unit.PLURAL_TERM + "/" + otherClientsUnit.id.uuidValue() , [
            id: '' + otherClientsUnit.id.uuidValue(),
            name: 'hijacked-unit',
            parent: [displayName: 'Test unit',
                targetUri: 'http://localhost//units/' + unit.id.value()]
        ], headers, 400)
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
        putEntityToAnotherUnit("/" + Asset.PLURAL_TERM, otherClientsAsset.getId().uuidValue())
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
        putEntityToAnotherUnit("/" + Control.PLURAL_TERM, otherClientsControl.getId().uuidValue())
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
        putEntityToAnotherUnit("/" + Person.PLURAL_TERM, otherClientsPerson.getId().uuidValue())
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
        putEntityToAnotherUnit("/" + Process.PLURAL_TERM, otherClientsProcess.getId().uuidValue())
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
        delete("/" + Unit.PLURAL_TERM + "/" + otherClientsUnit.getId().uuidValue(), 400)
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
        delete("/" + Asset.PLURAL_TERM + "/" + otherClientsAsset.getId().uuidValue(), 400)
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
        delete("/" + Control.PLURAL_TERM + "/" + otherClientsControl.getId().uuidValue(), 400)
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
        delete("/" + Person.PLURAL_TERM + "/" + otherClientsPerson.getId().uuidValue(), 400)
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
        delete("/" + Process.PLURAL_TERM + "/" + otherClientsProcess.getId().uuidValue(), 400)
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
        get("/" + Asset.PLURAL_TERM + "/" + otherClientsAsset.getId().uuidValue(), 400)
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
        get("/" + Control.PLURAL_TERM + "/" + otherClientsControl.getId().uuidValue(), 400)
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
        get("/" + Person.PLURAL_TERM + "/" + otherClientsPerson.getId().uuidValue(), 400)
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
        get("/" + Process.PLURAL_TERM + "/" + otherClientsProcess.getId().uuidValue(), 400)
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
        get("/" + Unit.PLURAL_TERM + "/" + otherClientsUnit.getId().uuidValue(), 400)
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
                targetUri: 'http://localhost/units/'+unit.id.uuidValue()
            ],
            links: [
                'process_dataType': [
                    [
                        target:
                        [
                            targetUri: "http://localhost/assets/$otherClientsAsset.dbId"
                        ]
                    ]
                ]
            ]
        ], 400)
        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }

    def postEntityInAnotherUnit(String url) {
        post(url, [
            name: 'entity-in-another client',
            owner: [displayName: '' + otherClientsUnit.name,
                targetUri: 'http://localhost/units/' + otherClientsUnit.id.value()]
        ],400)
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
        ], headers, 400)
    }
}
