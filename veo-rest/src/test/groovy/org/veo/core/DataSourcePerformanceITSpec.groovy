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

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles

import org.veo.core.entity.*
import org.veo.core.entity.Versioned.Lifecycle
import org.veo.core.entity.groups.PersonGroup
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.*

@SpringBootTest(classes = DataSourcePerformanceITSpec.class)
@ComponentScan("org.veo")
@ActiveProfiles(["test", "stats"])
class DataSourcePerformanceITSpec extends VeoSpringSpec {


    public static final String PROP_KEY = "propKey"
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
        assertUpdateCount(0)
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
        assertInsertCount(11)
        assertUpdateCount(1)
        assertSelectCount(7)
    }


    def "SQL performance for saving 1 process with 10 customAspects"() {
        given:
        createClient()

        when:
        reset()
        saveProcessWithCustomAspects(10)

        then:
        assertDeleteCount(0)
        assertInsertCount(3)
        assertUpdateCount(0)
        assertSelectCount(2)
    }

    def "SQL performance for saving 1 process with 1 customAspect with 10 array properties"() {
        given:
        createClient()

        when:
        reset()
        def process = saveProcessWithArrayCustomAspect(10)

        then:
        assertDeleteCount(0)
        assertInsertCount(14)
        assertUpdateCount(0)
        assertSelectCount(3)
    }

    def "SQL performance for putting 1 string value in 1 customAspect with 10 existing values"() {
        given:
        createClient()
        def process = saveProcessWithArrayCustomAspect(10)

        when:
        reset()
        updateProcessWithArrayCustomAspect(process)

        then:
        assertDeleteCount(0)
        assertInsertCount(1)
        assertUpdateCount(0)
        assertSelectCount(9)
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
        assertUpdateCount(1)
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
        assertDeleteCount(0)
        assertInsertCount(600)
        assertUpdateCount(101)
        assertSelectCount(310)
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
        unitRepository.delete(unit)

        then:
        assertDeleteCount(709)
        assertInsertCount(0)
        assertUpdateCount(0)
        assertSelectCount(3)
    }

    def "SQL performance for deleting a unit with 1 asset, 1 process and 1 persongroup linked to each other"() {
        given:
        createClient()
        createLinkedEntities()

        when:
        reset()
        deleteUnit()

        then: "all entities are removed"
        personRepository.findByUnits([unit] as Set).size() == 0
        assetRepository.findByUnits([unit] as Set).size() == 0
        processRepository.findByUnits([unit] as Set).size() == 0
        unitRepository.findByClient(client).size() == 0

        and:
        assertDeleteCount(30)
        assertInsertCount(0)
        assertUpdateCount(0)
        assertSelectCount(19)
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
        units.each { it.addToDomains(domain2) }
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
        def subGroups = []
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
            subGroups.add(subGroup)
        }
        group.getMembers().addAll(subGroups)
        personRepository.save(group)
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    List<Unit> selectSubUnits(Unit owner) {
        unitRepository.findByParent(owner)
    }

    @Transactional
    def Process saveProcessWithArrayCustomAspect(int count) {
        def process = newProcess(unit)
        def values = []
        for (i in 0..<count) {
            values.add("value_" + i)
        }
        CustomProperties cp = entityFactory.createCustomProperties().with {
            type = "aType"
            applicableTo = ["Process"] as Set
            it
        }
        cp.setProperty(PROP_KEY, values as List)
        process.addToCustomAspects(cp)
        return processRepository.save(process)
    }

    @Transactional
    def void updateProcessWithArrayCustomAspect(Process detachedProcess) {
        def process = processRepository.findById(detachedProcess.getId()).get()
        // adding 1 value to the end of the list:
        process.customAspects
                .first().stringListProperties.entrySet().first().value.add("value_new")
        processRepository.save(process)
    }

    @Transactional
    def createLinkedEntities() {
        def personGroup = savePersonGroup("parentgroup") // creates group with 2 persons

        def asset = newAsset(unit)
        asset = assetRepository.save(asset)

        def asset2 = newAsset(unit)
        asset2 = assetRepository.save(asset2)

        def process = newProcess(unit)
        process = processRepository.save(process)

        def link_person_asset = entityFactory.createCustomLink("link1", personGroup, asset).with {
            type = "type1"
            applicableTo = ["Person"] as Set
            it
        }
        personGroup.addToLinks(link_person_asset)
        personGroup = personRepository.save(personGroup)

        def link_asset_person = entityFactory.createCustomLink("link2", asset, personGroup).with {
            type = "type2"
            applicableTo = ["Asset"] as Set
            it
        }
        def link_asset_process = entityFactory.createCustomLink("link3", asset, process).with {
            type = "type3"
            applicableTo = ["Asset"] as Set
            it
        }
        asset.addToLinks(link_asset_process)
        asset.addToLinks(link_asset_person)
        asset = assetRepository.save(asset)

        def link_process_person = entityFactory.createCustomLink("link4", process, personGroup).with {
            type = "type4"
            applicableTo = ["Process"] as Set
            it
        }
        process.addToLinks(link_process_person)
        process = processRepository.save(process)

        def link_asset_asset = entityFactory.createCustomLink("link5", asset, asset2).with {
            type = "type5"
            applicableTo = ["Asset"] as Set
            it
        }
        asset.addToLinks(link_asset_asset)
        asset = assetRepository.save(asset)
    }

    @Transactional
    def deleteUnit() {
        assetRepository.deleteByUnit(unit)
        personRepository.deleteByUnit(unit)
        processRepository.deleteByUnit(unit)
        unitRepository.delete(unit)
    }
}
