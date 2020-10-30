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
import org.veo.core.entity.Versioned.Lifecycle
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

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
        def client = newClient()
        def unit = newUnit(client)

        clientRepository.save(client)
        unitRepository.save(unit)
        when:
        def personGroupId = Key.newUuid()

        def john = newPerson(unit)
        def jane = newPerson(unit)

        def personGroup = factory.createPersonGroup(personGroupId, 'My person group', unit)

        personGroup.with {
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
