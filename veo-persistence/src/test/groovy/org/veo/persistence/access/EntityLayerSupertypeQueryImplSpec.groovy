/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.persistence.access

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.veo.core.entity.Asset
import org.veo.core.repository.EntityLayerSupertypeQuery
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PagingConfiguration.SortOrder
import org.veo.core.repository.QueryCondition
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.UnitData

class EntityLayerSupertypeQueryImplSpec extends AbstractJpaSpec {

    @Autowired
    ProcessDataRepository processDataRepository

    @Autowired
    PersonDataRepository personDataRepository

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    UnitDataRepository unitDataRepository

    ClientData client
    UnitData unit
    EntityLayerSupertypeQuery<Asset> query

    def setup() {
        client = clientDataRepository.save(newClient {})
        unit = unitDataRepository.save(newUnit(client))

        query = new EntityLayerSupertypeQueryImpl<>(processDataRepository, client)
    }

    def 'queries by client'() {
        given:
        def otherClient = clientDataRepository.save(newClient {})
        def otherClientUnit = unitDataRepository.save(newUnit(otherClient))
        processDataRepository.saveAll([
            newProcess(unit) {name = "client process"},
            newProcess(otherClientUnit) {
                name = "other client process"
            }
        ])

        when:
        def result = query.execute(PagingConfiguration.UNPAGED)
        then:
        result.totalResults == 1
        result.resultPage.first().name == "client process"
    }

    def 'queries all processes'() {
        given:
        processDataRepository.saveAll([
            newProcess(unit),
            newProcess(unit),
            newProcess(unit)
        ])
        when:
        def all = query.execute(PagingConfiguration.UNPAGED)
        then:
        all.totalResults == 3
    }

    def 'queries by units'() {
        given:
        def unit2 = unitDataRepository.save(newUnit(client))
        def unit3 = unitDataRepository.save(newUnit(client))
        processDataRepository.saveAll([
            newProcess(unit) { name = "1st process" },
            newProcess(unit2) { name = "2nd process" },
            newProcess(unit3) { name = "3rd process" },
        ])

        when:
        query.whereUnitIn([unit, unit3] as Set)
        def result = query.execute(PagingConfiguration.UNPAGED)
        then:
        result.totalResults == 2
        with(result.resultPage) {
            it.size() == 2
            it[0].name == "1st process"
            it[1].name == "3rd process"
        }
    }

    def 'queries by sub type'() {
        given:
        def domain = domainRepository.save(newDomain {owner = this.client})

        processDataRepository.saveAll([
            newProcess(unit) {
                name = "a"
                setSubType(domain, "VT")
            },
            newProcess(unit) {
                name = "b"
                setSubType(domain, "VT")
            },
            newProcess(unit) {
                name = "c"
            }
        ])

        when:
        query.whereSubTypeMatches(new QueryCondition<>(["VT"] as Set))
        def result = query.execute(PagingConfiguration.UNPAGED)
        then:
        result.totalResults == 2
        with(result.resultPage) {
            it.size() == 2
            it[0].name == "a"
            it[1].name == "b"
        }
    }

    def 'finds processes with no sub type'() {
        given:
        def domain = domainRepository.save(newDomain {owner = this.client})

        processDataRepository.saveAll([
            newProcess(unit) {
                name = "a"
                setSubType(domain, "VT")
            },
            newProcess(unit) {
                name = "b"
                setSubType(domain, null)
            },
            newProcess(unit) {
                name = "c"
            }
        ])

        when:
        query.whereSubTypeMatches(new QueryCondition<>([null] as Set))
        def result = query.execute(PagingConfiguration.UNPAGED)
        then:
        result.totalResults == 2
        with(result.resultPage) {
            it.size() == 2
            it[0].name == "b"
            it[1].name == "c"
        }
    }

    def 'queries by unit & client'() {
        given:
        def client2 = clientDataRepository.save(newClient {})
        def unit2 = unitDataRepository.save(newUnit(client))
        def unit3 = unitDataRepository.save(newUnit(client2))
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "process 1"
            },
            newProcess(unit2) {
                name = "process 2"
            },
            newProcess(unit3) {
                name = "process 3"
            }
        ])

        when:
        query.whereUnitIn([unit2, unit3] as Set)
        def result = query.execute(PagingConfiguration.UNPAGED)
        then:

        result.totalResults == 1
        result.resultPage.first().name == "process 2"
    }

    def 'queries by name'() {
        given:
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "Process One"
            },
            newProcess(unit) {
                name = "Process Two"
            },
            newProcess(unit) {
                name = "Whatever"
            }
        ])

        when:
        query.whereNameMatchesIgnoreCase(new QueryCondition(Set.of("two", "what")))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage.sort{it.name}*.name == ["Process Two", "Whatever"]
    }

    def 'queries by description'() {
        given:
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "A"
                description = "I am number one"
            },
            newProcess(unit) {
                name = "B"
                description = "I am number two"
            },
            newProcess(unit) {
                name = "C"
                description = "I am number three"
            }
        ])

        when:
        query.whereDescriptionMatchesIgnoreCase(new QueryCondition(Set.of("AM NUMBER TWO", "THREE")))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage.sort{it.name}*.name == ["B", "C"]
    }

    def 'queries by designator'() {
        given:
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "A"
                designator = "PER-1"
            },
            newProcess(unit) {
                name = "B"
                designator = "PER-53"
            },
            newProcess(unit) {
                name = "C"
                designator = "PER-899"
            }
        ])

        when:
        query.whereDesignatorMatchesIgnoreCase(new QueryCondition(Set.of("53", "per-8")))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage.sort{it.name}*.name == ["B", "C"]
    }

    def 'queries by updatedBy'() {
        given:
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "A"
                updatedBy = "Max Muster"
            },
            newProcess(unit) {
                name = "B"
                updatedBy = "Max Schuster"
            },
            newProcess(unit) {
                name = "C"
                updatedBy = "Martha Muster"
            }
        ])

        when:
        query.whereUpdatedByContainsIgnoreCase(new QueryCondition(Set.of("schuster", "martha ")))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage.sort{it.name}*.name == ["B", "C"]
    }

    def 'sort results by different properties'() {
        given: "three processes"

        processDataRepository.saveAll([
            newProcess(unit) {
                name = "process 0"
                description = 'y'
            },
            newProcess(unit) {
                name = "process 1"
                description = 'z'
            },
            newProcess(unit) {
                name = "process 2"
                description = 'x'
            }
        ])

        when: "querying processes sorted by name ascending"
        def result = query.execute(new PagingConfiguration(3, 0, 'name', SortOrder.ASCENDING))

        then: "the sort order is correct"
        with(result.resultPage) {
            size() == 3
            it[0].name == "process 0"
            it[1].name == "process 1"
            it[2].name == "process 2"
        }

        when: "querying processes sorted by name descending"
        result = query.execute( new PagingConfiguration(3, 0, 'name', SortOrder.DESCENDING))

        then: "the sort order is correct"
        with(result.resultPage) {
            size() == 3
            it[0].name == "process 2"
            it[1].name == "process 1"
            it[2].name == "process 0"
        }

        when: "querying processes sorted by description ascending"
        result = query.execute( new PagingConfiguration(3, 0, 'description', SortOrder.ASCENDING))

        then: "the sort order is correct"
        with(result.resultPage) {
            size() == 3
            it[0].name == "process 2"
            it[1].name == "process 0"
            it[2].name == "process 1"
        }
    }

    def 'Paging configuration is correctly passed to data repository'() {
        given: 'a repository'
        AssetDataRepository dataRepository = Mock()
        def query = new EntityLayerSupertypeQueryImpl<>(dataRepository, client)
        when:
        def result = query.execute(new PagingConfiguration(2, 0, 'foo', SortOrder.ASCENDING))
        then:
        1 * dataRepository.findAll(_, { Pageable pageable->
            pageable.pageSize == 2
            pageable.pageNumber == 0
            pageable.sort.size() == 1
            pageable.sort.first().ascending
            pageable.sort.first().property == 'foo'
        }) >> Page.empty()
        1 * dataRepository.findAllById(_) >> []
    }

    def 'sort by designator produces expected sorting'() {
        given:
        personDataRepository.saveAll((1..100).collect{ n->
            newPerson(unit) {
                name = "Person $n"
                designator = "PER-$n"
            }
        })
        when: "querying processes sorted by designator ascending"
        def query = new EntityLayerSupertypeQueryImpl<>(personDataRepository, client)

        def result = query.execute(new PagingConfiguration(100, 0, 'designator', SortOrder.ASCENDING))

        then: "the sort order is correct"
        with(result.resultPage) {
            it[0].name == "Person 1"
            it[22].name == "Person 23"
            it[41].name == "Person 42"
            it[99].name == "Person 100"
        }
    }
}
