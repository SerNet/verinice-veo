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
package org.veo.core

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

import org.veo.core.entity.Key
import org.veo.core.entity.ModelObject.Lifecycle
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.UnitData

@SpringBootTest(classes = GroupPersistenceSpec.class)
@Transactional()
@ComponentScan("org.veo")
class GroupPersistenceSpec extends VeoSpringSpec {


    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    private EntityFactory factory



    def "save a person group"() {
        given: "a client and a unit"
        Key clientId = Key.newUuid()

        ClientData client = new ClientData()
        client.dbId = clientId.uuidValue()

        def unitId = Key.newUuid()

        def unit = new UnitData()
        unit.id = unitId
        unit.name = "u-1"
        unit.client = client

        clientRepository.save(client)
        unitRepository.save(unit)
        when:
        def personGroupId = Key.newUuid()

        def john = factory.createPerson(Key.newUuid(),'John',unit)
        def jane = factory.createPerson(Key.newUuid(),'Jane',unit)

        def personGroup = factory.createPersonGroup()

        personGroup.with {
            name = 'My person group'
            owner = unit
            id = personGroupId
            members = [john, jane]
            state = Lifecycle.CREATING
            it
        }
        personGroup = personRepository.save(personGroup)
        then:
        personGroup != null
        personGroup.members  == [john, jane] as Set

        when:
        personGroup = personRepository.findById(personGroupId)
        then:
        personGroup.present
        personGroup.get().name == 'My person group'
        personGroup.get().members == [john, jane] as Set
    }
}
