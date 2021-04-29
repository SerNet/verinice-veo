/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

import org.veo.core.entity.Versioned.Lifecycle
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@SpringBootTest(classes = PersonRepositoryITSpec.class)
@Transactional()
@ComponentScan("org.veo")
class PersonRepositoryITSpec extends VeoSpringSpec {


    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    def "save a composite person"() {
        given: "a client and a unit"
        def client = clientRepository.save(newClient())
        def unit = unitRepository.save(newUnit(client))

        when:

        def john = newPerson(unit)
        def jane = newPerson(unit)

        def compositePerson = newPerson(unit) {
            name = 'My composite person'
        }

        compositePerson.with {
            parts = [john, jane]
            state = Lifecycle.CREATING
            it
        }
        compositePerson = personRepository.save(compositePerson)
        then:
        compositePerson != null
        compositePerson.parts  == [john, jane] as Set

        when:
        compositePerson = personRepository.findById(compositePerson.id)
        then:
        compositePerson.present
        compositePerson.get().name == 'My composite person'
        compositePerson.get().parts == [john, jane] as Set
    }
}
