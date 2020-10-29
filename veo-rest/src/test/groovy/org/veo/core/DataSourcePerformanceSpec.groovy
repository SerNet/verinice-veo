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

import static com.vladmihalcea.sql.SQLStatementCountValidator.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned.Lifecycle
import org.veo.core.entity.groups.PersonGroup
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.CustomPropertiesData
import org.veo.persistence.entity.jpa.UnitData

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
    private AssetRepositoryImpl assetRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private EntityFactory entityFactory

    private Client client
    private Unit unit

    static final String USERNAME = "Guybrush"


    def "SQL performance for saving a new domain, client and unit"() {
        when:
        reset()
        createClient()

        then:
        assertDeleteCount(0)
        assertInsertCount(4)
        assertUpdateCount(1)
        assertSelectCount(4)
    }

    def "SQL performance for saving 1 process"() {
        given:
        createClient()

        when:
        reset()
        saveProcess("process1")

        then:
        assertDeleteCount(0)
        assertInsertCount(1)
        assertUpdateCount(1)
        assertSelectCount(1)
    }

    def "SQL performance for saving 1 process with 2 links to 1 asset and 1 personGroup"() {
        given:
        createClient()

        when:
        reset()
        saveProcessWithLinks()

        then:
        assertDeleteCount(0)
        assertInsertCount(13)
        assertUpdateCount(5)
        assertSelectCount(9)
    }


    def "SQL performance for saving 1 process with 10 customAspects"() {
        given:
        createClient()

        when:
        reset()
        saveProcessWithCustomAspects(10)

        then:
        assertDeleteCount(0)
        assertInsertCount(4)
        assertUpdateCount(1)
        assertSelectCount(2)
    }

    def "SQL performance for saving 1 group of 2 persons"() {
        given:
        createClient()

        when:
        reset()
        savePersonGroup("personGroup1")

        then:
        assertDeleteCount(0)
        assertInsertCount(5)
        assertUpdateCount(3)
        assertSelectCount(3)
    }


    def "SQL performance for saving 1 group with 100 subgroups"() {
        given:
        createClient()
        def group = savePersonGroup("parentgroup")

        when:
        reset()
        savePersonSubGroups(100, "subgroup", group as PersonGroup)

        then:
        assertDeleteCount(101)
        assertInsertCount(600)
        assertUpdateCount(401)
        assertSelectCount(1209)
    }

    def "SQL performance for selecting units of a client"() {
        given:
        createClient()
        createClientUnits(100)

        when:
        reset()
        def units = selectUnits()

        then:
        units.size() == 101
        assertDeleteCount(0)
        assertInsertCount(0)
        assertUpdateCount(0)
        assertSelectCount(1)
    }

    def "SQL performance for selecting subunits of a unit"() {
        given:
        createClient()
        createSubUnits(100)

        when:
        reset()
        def units = selectSubUnits(unit)

        then:
        units.size() == 100
        assertDeleteCount(0)
        assertInsertCount(0)
        assertUpdateCount(0)
        assertSelectCount(2)
    }

    def "SQL performance for deleting 1 unit with 100 groups of 2 persons each"() {
        given:
        createClient()
        def group = savePersonGroup("parentgroup")
        savePersonSubGroups(100, "subgroup", group as PersonGroup)

        when:
        reset()
        personRepository.deleteByUnit(unit)

        then:
        assertDeleteCount(707)
        assertInsertCount(0)
        assertUpdateCount(0)
        assertSelectCount(911)
    }

    def "SQL performance for deleting 2 units with 1 commonly referenced domain"() {
        given:
        createClient()
        def units = createClientUnits(2)
        def domain2 = entityFactory.createDomain()
        domain2.setId(Key.newUuid())
        domain2.setName("domain2")
        domain2.version(USERNAME, null)
        client.addToDomains(domain2)
        client = clientRepository.save(client)

        when:
        reset()
        units.each {it.addToDomains(domain2)}
        unitRepository.saveAll(units)

        then:
        assertDeleteCount(0)
        assertInsertCount(2)
        assertUpdateCount(2)
        assertSelectCount(5)
    }

    @Transactional
    void createClient() {
        def domain = entityFactory.createDomain()
        domain.setId(Key.newUuid())
        domain.setName("domain1")
        domain.version(USERNAME, null)

        client = entityFactory.createClient()
        client.addToDomains(domain)
        client.setId(Key.newUuid())
        client.version(USERNAME, null)

        unit = entityFactory.createUnit()
        unit.setClient(client)
        unit.setId(Key.newUuid())
        unit.setName("unit1")
        unit.addToDomains(domain)
        unit.version(USERNAME, null)

        client = clientRepository.save(client)
        unit = unitRepository.save(unit)
    }

    @Transactional
    Asset saveAsset(String assetName) {
        return assetRepository.save(newAsset(unit).with {
            name = assetName
            it
        })
    }

    @Transactional
    Process saveProcess(String processName) {
        return processRepository.save(newProcess(unit).with {
            name = processName
            it
        })
    }

    @Transactional
    Process saveProcessWithLinks() {
        def asset = saveAsset("asset1")
        def process = newProcess(unit).with {
            name = "process1"
            it
        }
        def personGroup = savePersonGroup("persongroup1")

        def link1 = entityFactory.createCustomLink("aLink", process, asset).with {
            type = "aLink"
            applicableTo = ["Process"] as Set
            it
        }

        def link2 = entityFactory.createCustomLink("anotherLink", process, personGroup).with {
            type = "anotherLink"
            applicableTo = ["Process"] as Set
            it
        }

        process.setLinks([link1, link2] as Set)
        processRepository.save(process)
    }

    @Transactional
    Person savePersonGroup(String groupName) {
        def personGroupId = Key.newUuid()
        def john = newPerson(unit)
        def jane = newPerson(unit)
        def personGroup = entityFactory.createPersonGroup()

        personGroup.with {
            name = groupName
            owner = unit
            id = personGroupId
            members = [john, jane]
            state = Lifecycle.CREATING
            it
        }
        personGroup.version(USERNAME, null)
        return personRepository.save(personGroup)
    }

    @Transactional
    def savePersonSubGroups(int count, String baseName, PersonGroup group) {
        for (i in 0..<count) {
            def personGroupId = Key.newUuid()
            def dolly = newPerson(unit)
            def minime = newPerson(unit)
            def subGroup = entityFactory.createPersonGroup()
            subGroup.with {
                name = baseName + count
                owner = unit
                id = personGroupId
                members = [dolly, minime]
                state = Lifecycle.CREATING
                it
            }
            subGroup.version(USERNAME, null)
            subGroup = personRepository.save(subGroup)
            group.getMembers().add(subGroup)
        }
        personRepository.save(group)
    }

    def saveProcessWithCustomAspects(int count) {
        def process = newProcess(unit)
        for (i in 0..<count) {
            CustomProperties cp = entityFactory.createCustomProperties().with {
                type = "aType"
                applicableTo = ["Process"] as Set
                it
            }
            process.addToCustomAspects(cp)
        }
        processRepository.save(process)
    }

    @Transactional
    HashSet<Unit> createClientUnits(int count) {
        def units = new HashSet<Unit>()
        for (i in 0..<count) {
            def unit = newUnit(client).with {
                name = "unit" + count
                it
            }
            units.add(unitRepository.save(unit))
        }
        units
    }

    @Transactional
    selectUnits() {
        unitRepository.findByClient(client)
    }

    void createSubUnits(int count) {
        for (i in 0..<count) {
            def unit = newUnit(client).with {
                name = "unit" + count
                parent = unit
                it
            }
            unitRepository.save(unit)
        }
    }

    List<Unit> selectSubUnits(Unit owner) {
        unitRepository.findByParent(owner)
    }
}
