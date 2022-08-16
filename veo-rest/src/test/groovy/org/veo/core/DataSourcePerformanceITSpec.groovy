/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.CompositeElement
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.repository.PagingConfiguration
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.StoredEventDataRepository

import spock.lang.Issue

class DataSourcePerformanceITSpec extends AbstractPerformaceITSpec {

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
    private ScopeRepositoryImpl scopeRepository

    @Autowired
    private StoredEventDataRepository storedEventRepository

    private Client client
    private Unit unit
    Domain domain

    def "SQL performance for saving a new domain, client and unit"() {
        when:
        def queryCounts = trackQueryCounts{
            createClient()
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 6
        queryCounts.update == 0
        queryCounts.select == 3
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 process"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveProcess("process1")
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 2
        queryCounts.update == 0
        queryCounts.select == 0
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 process with 2 links to 1 asset and 1 composite person"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveProcessWithLinks()
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 5
        queryCounts.update == 0
        queryCounts.select == 0
        queryCounts.time < 500
    }


    def     "SQL performance for saving 1 process with 10 customAspects"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveProcessWithCustomAspects(10)
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 3
        queryCounts.update == 0
        queryCounts.select == 0
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 process with 1 custom aspect"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveProcessWithCustomAspect()
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 3
        queryCounts.update == 0
        queryCounts.select == 0
        queryCounts.time < 500
    }

    def "SQL performance for selecting 1 process with embedded risk"() {
        given:
        createClientWithRiskDefinition()
        def process = saveProcessWithRisk()

        when: "fetch risks using full aggregate graph with risks"
        def rd = new RiskDefinitionRef("r2d2")
        def processes
        def queryCounts = trackQueryCounts{
            processes = selectProcesses(process.id, true)
            processes.first().risks.forEach{
                it.getRiskProvider(rd, domain)
                        .getCategorizedRisks().size() == 4
                def prob = it.getProbabilityProvider(rd, domain).effectiveProbability
                def cat = it.getRiskProvider(rd, domain).getAvailableCategories().first()
                def risk = it.getRiskProvider(rd, domain).getInherentRisk(cat)
                def scenario = it.getScenario().getDisplayName()
                def person = it.getRiskOwner().getDisplayName()
                def mitigation = it.getMitigation().getDisplayName()
                def customAttributes = process.customAspects.first().attributes
            }
        }

        then: "all risks elements were fetched"
        queryCounts.delete == 0
        queryCounts.insert == 0
        queryCounts.update == 0
        queryCounts.select == 2
        queryCounts.time < 500

        when: "fetch risks using only full aggregate graph"
        queryCounts = trackQueryCounts{
            processes = selectProcesses(process.id, false)
            processes.first().risks.forEach{
                it.getRiskProvider(rd, domain)
                        .getCategorizedRisks().size() == 4
                def prob = it.getProbabilityProvider(rd, domain).effectiveProbability
                def cat = it.getRiskProvider(rd, domain).getAvailableCategories().first()
                def risk = it.getRiskProvider(rd, domain).getInherentRisk(cat)
                def scenario = it.getScenario().getDisplayName()
                def person = it.getRiskOwner().getDisplayName()
                def mitigation = it.getMitigation().getDisplayName()
                def customAttributes = process.customAspects.first().attributes
            }
        }

        then: "risk relations had to be selected additionally"
        queryCounts.delete == 0
        queryCounts.insert == 0
        queryCounts.update == 0
        queryCounts.select == 7
        queryCounts.time < 500
    }

    def "SQL performance for putting 1 string value in 1 customAspect with 10 existing values"() {
        given:
        createClient()
        def process = saveProcessWithCustomAspect()

        when:
        def queryCounts = trackQueryCounts{
            updateProcessWithCustomAspect(process)
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 0
        queryCounts.update == 1
        queryCounts.select == 1
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 composite person with 2 parts"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            savePerson([
                newPerson(unit),
                newPerson(unit)
            ])
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 3
        queryCounts.update == 0
        queryCounts.select == 0
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 scope with 2 persons"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveScope("Scope 1", [
                newPerson(unit),
                newPerson(unit)
            ])
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 4
        queryCounts.update == 0
        queryCounts.select == 0
        queryCounts.time < 500
    }


    def     "SQL performance for saving 1 scope with 100 persons"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveScope("Scope 1",(1..100).collect{
                newPerson(unit)
            })
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 13
        queryCounts.update == 0
        queryCounts.select == 2
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 scope with 100 persons with 2 parts each"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveScope("Scope 1",(1..100).collect{
                newPerson(unit) {
                    parts = [
                        newPerson(unit),
                        newPerson(unit)
                    ]
                }
            })
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 33
        queryCounts.update == 0
        queryCounts.select == 6
        queryCounts.time < 500
    }

    def "SQL performance for saving 1 scope with 10 persons with 10 parts each"() {
        given:
        createClient()

        when:
        def queryCounts = trackQueryCounts{
            saveScope("Scope 1",(1..10).collect{
                newPerson(unit) {
                    parts =(1..10).collect{
                        newPerson(unit)
                    }
                }
            })
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 14
        queryCounts.update == 0
        queryCounts.select == 2
        queryCounts.time < 500
    }

    def "SQL performance for adding 100 persons with 2 parts each to an existing scope"() {
        given:
        createClient()
        def scope = saveScope("scope")

        when:
        def queryCounts = trackQueryCounts{
            saveWithAddedCompositePersons(100, "person", scope )
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 35
        queryCounts.update == 5
        queryCounts.select == 19
        queryCounts.time < 500
    }

    def "SQL performance for selecting units of a client"() {
        given:
        createClient()
        createClientUnits(100)

        when:
        def units
        def queryCounts = trackQueryCounts{
            units = selectUnits()
        }
        then:
        units.size() == 101
        queryCounts.delete == 0
        queryCounts.insert == 0
        queryCounts.update == 0
        queryCounts.select == 1
        queryCounts.time < 500
    }

    def "SQL performance for selecting subunits of a unit"() {
        given:
        createClient()
        createSubUnits(100)

        when:
        def units
        def queryCounts = trackQueryCounts{
            units = selectSubUnits(unit)
        }
        then:
        units.size() == 100
        queryCounts.delete == 0
        queryCounts.insert == 0
        queryCounts.update == 0
        queryCounts.select == 2
        queryCounts.time < 500
    }

    def "SQL performance for deleting 1 unit with 100 persons of 2 parts each"() {
        given:
        createClient()
        100.times {
            savePerson([
                newPerson(unit),
                newPerson(unit)
            ])
        }

        when:
        def queryCounts = trackQueryCounts{
            personRepository.deleteAll(personRepository.findByUnit(unit))
            unitRepository.delete(unit)
        }
        then:
        queryCounts.delete == 16
        queryCounts.insert == 11
        queryCounts.update == 0
        queryCounts.select == 21
        queryCounts.time < 1000
    }

    @Issue('VEO-689')
    def "SQL performance for deleting a unit with 1 asset, 1 process and 1 composite person linked to each other"() {
        given:
        createClient()
        createLinkedElements()

        when:
        def queryCounts = trackQueryCounts{
            deleteUnit()
        }

        then: "all elements are removed"
        executeInTransaction {
            personRepository.query(client)
                    .whereUnitIn([unit] as Set)
                    .execute(PagingConfiguration.UNPAGED)
        }.totalResults == 0

        executeInTransaction {
            assetRepository.query(client)
                    .whereUnitIn([unit] as Set)
                    .execute(PagingConfiguration.UNPAGED)
        }.totalResults == 0

        executeInTransaction {
            processRepository.query(client)
                    .whereUnitIn([unit] as Set)
                    .execute(PagingConfiguration.UNPAGED)
        }.totalResults == 0

        executeInTransaction {
            unitRepository.findByClient(client).size()
        } == 0

        and:
        queryCounts.delete <= 12
        queryCounts.insert == 4
        queryCounts.update == 2
        queryCounts.select == 45
        queryCounts.time < 500
    }

    def "SQL performance for deleting 2 units with 1 commonly referenced domain"() {
        given:
        createClient()
        def units = createClientUnits(2)

        def domain2 = newDomain(client) {
            authority = 'ta'
            revision = '1'
            templateVersion = '1.0'
        }

        client = clientRepository.save(client)

        when:
        def queryCounts = trackQueryCounts{
            units.each {
                it.addToDomains(domain2)
            }
            unitRepository.saveAll(units)
        }
        then:
        queryCounts.delete == 0
        queryCounts.insert == 2
        queryCounts.update == 1
        queryCounts.select == 6
        queryCounts.time < 500
    }

    void createClient() {
        executeInTransaction {
            client = clientRepository.save(newClient() {
                newDomain(it)
            })

            def domain = client.domains.first()

            unit = newUnit(client)
            unit.setClient(client)
            unit.addToDomains(domain)

            unit = unitRepository.save(unit)
        }
    }

    void createClientWithRiskDefinition() {
        executeInTransaction {
            client = clientRepository.save(newClient() {
                newDomain(it) {
                    it.riskDefinitions = [ "r2d2":
                        createRiskDefinition("r2d2")
                    ] as Map
                }
            })

            domain = client.domains.first()

            unit = newUnit(client)
            unit.setClient(client)
            unit.addToDomains(domain)

            unit = unitRepository.save(unit)
        }
    }

    Asset saveAsset(String assetName) {
        executeInTransaction {
            return assetRepository.save(newAsset(unit).with {
                name = assetName
                it
            })
        }
    }

    Process saveProcess(String processName) {
        executeInTransaction {
            return processRepository.save(newProcess(unit).tap {
                name = processName
            })
        }
    }

    Scope saveScope(String name, List<CompositeElement<CompositeElement>> members = []) {
        executeInTransaction {
            return scopeRepository.save(newScope(unit).tap {
                it.name = name
                it.members = members
            })
        }
    }

    Process saveProcessWithLinks() {
        executeInTransaction {
            def asset = saveAsset("asset1")
            def process = newProcess(unit).with {
                name = "process1"
                it
            }
            def person = savePerson()

            process.setLinks([
                newCustomLink(asset, "aLink"),
                newCustomLink(person, "anotherLink")
            ] as Set)
            processRepository.save(process)
        }
    }

    Person savePerson(List<Person> parts = []) {
        executeInTransaction {
            def person = newPerson(unit) {
                it.parts = parts
            }
            return personRepository.save(person)
        }
    }

    def saveWithAddedCompositePersons(int count, String baseName, Scope scope ) {
        executeInTransaction {
            def compositePersons = (0..<count).collect {
                def dolly = newPerson(unit)
                def minime = newPerson(unit)
                def person = newPerson(unit) {
                    name = baseName+count
                }
                person.tap {
                    parts = [dolly, minime]
                }
            }

            scope.members.addAll(compositePersons)
            scopeRepository.save(scope)
        }
    }

    def saveProcessWithCustomAspects(int count) {
        executeInTransaction {
            def process = newProcess(unit)
            for (i in 0..<count) {
                CustomAspect customAspect = newCustomAspect("aType $i")
                process.addToCustomAspects(customAspect)
            }
            processRepository.save(process)
        }
    }

    HashSet<Unit> createClientUnits(int count) {
        executeInTransaction {
            def units = new HashSet<Unit>()
            for (i in 0..<count) {
                def unit = newUnit(client).tap {
                    name = "unit" + i
                }
                units.add(unitRepository.save(unit))
            }
            units
        }
    }

    def selectUnits() {
        executeInTransaction {
            unitRepository.findByClient(client)
        }
    }

    List<Process> selectProcesses(Key<UUID> id, boolean withRisks = false) {
        executeInTransaction {
            def result = processDataRepository.findAllById([id.uuidValue()])
            if (withRisks)
                processDataRepository.findAllWithRisksByDbIdIn([id.uuidValue()])
            result
        }
    }

    void createSubUnits(int count) {
        executeInTransaction {
            for (i in 0..<count) {
                def unit = newUnit(client).tap {
                    name = "unit" + i
                    parent = unit
                }
                unitRepository.save(unit)
            }
        }
    }

    List<Unit> selectSubUnits(Unit owner) {
        executeInTransaction {
            unitRepository.findByParent(owner)
        }
    }

    Process saveProcessWithCustomAspect() {
        executeInTransaction {
            def process = newProcess(unit)
            CustomAspect customAspect = newCustomAspect("aType")
            customAspect.attributes = [
                PROP_KEY: "ok"
            ]
            process.addToCustomAspects(customAspect)
            return processRepository.save(process)
        }
    }

    Process saveProcessWithRisk() {
        def process = saveProcessWithCustomAspect()
        executeInTransaction {
            def domain = client.domains.first()
            def scenario = scenarioDataRepository.save(newScenario(unit))
            def person = personDataRepository.save(newPerson(unit))
            def control = controlDataRepository.save(newControl(unit))
            process.associateWithDomain(domain, "NormalProcess", "NEW")
            scenario.associateWithDomain(domain, "NormalScenario", "NEW")
            process.obtainRisk(scenario, domain).tap{
                it.setDesignator("RSK-1")
                it.setCreatedBy("me")
                it.setUpdatedBy("someoneelse")
                it.mitigate(control)
                it.appoint(person)
            }
            process.risks.first().defineRiskValues([
                newRiskValues(new RiskDefinitionRef("r2d2"), domain)
            ] as Set)

            process = processDataRepository.save(process)
        }
        return process
    }

    void updateProcessWithCustomAspect(Process detachedProcess) {
        executeInTransaction {
            def process = processRepository.findById(detachedProcess.getId()).get()
            // adding 1 value to the end of the list:
            process.customAspects.first().attributes = [
                PROP_KEY: "updated val"
            ]
            processRepository.save(process)
        }
    }

    def createLinkedElements() {
        executeInTransaction {
            def compositePerson = savePerson([
                newPerson(unit),
                newPerson(unit)
            ]) // creates person with 2 parts

            def asset = newAsset(unit)
            asset = assetRepository.save(asset)

            def asset2 = newAsset(unit)
            asset2 = assetRepository.save(asset2)

            def process = newProcess(unit)
            process = processRepository.save(process)

            def link_person_asset = newCustomLink(asset, " type1")
            compositePerson.addToLinks(link_person_asset)
            compositePerson = personRepository.save(compositePerson)

            def link_asset_person = newCustomLink(compositePerson, "type2")
            def link_asset_process = newCustomLink(process, "type3")
            asset.addToLinks(link_asset_process)
            asset.addToLinks(link_asset_person)
            asset = assetRepository.save(asset)

            def link_process_person = newCustomLink(compositePerson, "type4")
            process.addToLinks(link_process_person)
            processRepository.save(process)

            def link_asset_asset = newCustomLink(asset2, "type5")
            asset.addToLinks(link_asset_asset)
            assetRepository.save(asset)
        }
    }

    def deleteUnit() {
        executeInTransaction {
            def assets = assetRepository.findByUnit(unit)
            def persons = personRepository.findByUnit(unit)
            def processes = processRepository.findByUnit(unit)
            def scopes = scopeRepository.findByUnit(unit)

            assetRepository.deleteAll(assets)
            personRepository.deleteAll(persons)
            processRepository.deleteAll(processes)
            scopeRepository.deleteAll(scopes)

            unitRepository.delete(unit)
        }
    }
}
