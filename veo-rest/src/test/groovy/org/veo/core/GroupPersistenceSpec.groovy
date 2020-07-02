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

import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.ModelObject.Lifecycle
import org.veo.core.entity.groups.PersonGroup
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl

@SpringBootTest(classes = GroupPersistenceSpec.class)
@Transactional()
@ComponentScan("org.veo")
class GroupPersistenceSpec extends VeoSpringSpec {


    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private PersonRepositoryImpl personRepository


    def "save a person group"() {
        given: "a client and a unit"
        Key clientId = Key.newUuid()

        Client client = newClient {
            id = clientId
        }

        def unitId = Key.newUuid()

        def unit = newUnit(client)
        client.addToUnits(unit)
        clientRepository.save(client)

        when:
        def personGroupId = Key.newUuid()

        def john = newPerson unit, {
            name = 'John'
        }
        def jane = newPerson unit, {
            name = 'Jane'
        }

        def personGroup = new PersonGroup().with {
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
