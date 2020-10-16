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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Versioned.Lifecycle
import org.veo.core.entity.groups.PersonGroup
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import static com.vladmihalcea.sql.SQLStatementCountValidator.*

@SpringBootTest(classes = DataSourcePerformanceSpec.class)
@ComponentScan("org.veo")
@ActiveProfiles(["test", "stats"])
class DataSourcePerformanceSpec extends VeoSpringSpec {


    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    private EntityFactory factory
    private client
    private unit

    @Transactional
    def createClient() {
        client = newClient()
        unit = newUnit(this.client)
        clientRepository.save(this.client)
        unitRepository.save(this.unit)

    }

    @Transactional
    Person savePersonGroup(String groupName) {
        def personGroupId = Key.newUuid()
        def john = newPerson(unit)
        def jane = newPerson(unit)
        def personGroup = factory.createPersonGroup()
        personGroup.with {
            name = groupName
            owner = unit
            id = personGroupId
            members = [john, jane]
            state = Lifecycle.CREATING
            it
        }
        return personRepository.save(personGroup)
    }
    @Transactional
    def savePersonSubGroups(int count, String baseName, PersonGroup group) {
        for (i in 0..<count) {
            def personGroupId = Key.newUuid()
            def dolly = newPerson(unit)
            def minime = newPerson(unit)
            def subGroup = factory.createPersonGroup()
            subGroup.with {
                name = baseName + count
                owner = unit
                id = personGroupId
                members = [dolly, minime]
                state = Lifecycle.CREATING
                it
            }
            subGroup = personRepository.save(subGroup)
            group.getMembers().add(subGroup)
        }
        personRepository.save(group)
    }

    def "SQL performance for saving a new client and unit"() {
        when:
        reset()
        createClient()

        then:
        assertDeleteCount(0)
        assertInsertCount(2)
        assertUpdateCount(0)
        assertSelectCount(2)

    }

    def "SQL performance for saving a group of persons"() {
        given:
        createClient()

        when:
        reset()
        savePersonGroup("personGroup1")

        then:
        assertDeleteCount(0)
        assertInsertCount(5)
        assertUpdateCount(3)
        assertSelectCount(0)
    }


    def "SQL performance for saving a complex structure with subgroups and links"() {
        given:
        createClient()
        def group = savePersonGroup("parentgroup")

        when:
        reset()
        savePersonSubGroups(100, "subgroup", group as PersonGroup)

        then:
        assertDeleteCount(0)
        assertInsertCount(0)
        assertUpdateCount(100)
        assertSelectCount(0)
    }

//    def "SQL performance for deleting a unit with many members"() {
//
//    }
//
//    def "SQL performance for selecting a list of units"() {
//
//    }
}
