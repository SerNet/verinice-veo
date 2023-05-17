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

import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PagingConfiguration.SortOrder
import org.veo.core.repository.QueryCondition
import org.veo.core.repository.SingleValueQueryCondition
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ElementDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.UnitData

class ElementQueryImplSpec extends AbstractJpaSpec {

    @Autowired
    ProcessDataRepository processDataRepository

    @Autowired
    ElementDataRepository elementDataRepository

    @Autowired
    PersonDataRepository personDataRepository

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    UnitDataRepository unitDataRepository

    @Autowired
    ScopeDataRepository scopeDataRepository

    @Autowired
    AssetDataRepository assetDataRepository

    ClientData client
    Domain domain
    UnitData unit

    def setup() {
        client = clientDataRepository.save(newClient{
            id = Key.newUuid()
            newDomain(it)
        })
        domain = client.domains.first()
        unit = unitDataRepository.save(newUnit(client))
    }

    def 'queries by client'() {
        given:
        def query = new ElementQueryImpl<>(processDataRepository, client)
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

    def 'queries composites by parts'() {
        given:
        def query = new ElementQueryImpl<>(processDataRepository, client)
        def part1 = processDataRepository.save(newProcess(unit))
        def part2 = processDataRepository.save(newProcess(unit))
        def part3 = processDataRepository.save(newProcess(unit))
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "composite of part 1 & 2"
                parts = [part1, part2]
            },
            newProcess(unit) {
                name = "composite of part 2"
                parts = [part2]
            },
            newProcess(unit) {
                name = "composite of part 3"
                parts = [part3]
            },
        ])

        when:
        query.whereChildElementIn(new QueryCondition<>([
            part1.id,
            part2.id,
        ] as Set))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage*.name =~ [
            "composite of part 1 & 2",
            "composite of part 2"
        ]
    }

    def 'queries composites by part presence'() {
        given:
        def part1 = processDataRepository.save(newProcess(unit) {
            name = "part one"
        })
        def part2 = processDataRepository.save(newProcess(unit) {
            name = "part two"
        })
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "composite with one part"
                parts = [part1]
            },
            newProcess(unit) {
                name = "composite with two parts"
                parts = [part1, part2]
            },
        ])

        when:
        def elementsWithChildren = new ElementQueryImpl<>(processDataRepository, client).with {
            whereChildElementsPresent(true)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        elementsWithChildren.resultPage*.name =~ [
            "composite with one part",
            "composite with two parts"
        ]

        when:
        def elementsWithoutChildren = new ElementQueryImpl<>(processDataRepository, client).with {
            whereChildElementsPresent(false)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        elementsWithoutChildren.resultPage*.name =~ [
            "part one",
            "part two",
        ]
    }

    def 'queries scopes by members'() {
        given:
        def query = new ElementQueryImpl(scopeDataRepository, client)
        def member1 = processDataRepository.save(newProcess(unit))
        def member2 = processDataRepository.save(newProcess(unit))
        def member3 = processDataRepository.save(newProcess(unit))
        scopeDataRepository.saveAll([
            newScope(unit) {
                name = "scope of member 1 & 2"
                members = [member1, member2]
            },
            newScope(unit) {
                name = "scope of member 2"
                members = [member2]
            },
            newScope(unit) {
                name = "scope of member 3"
                members = [member3]
            },
        ])

        when:
        query.whereChildElementIn(new QueryCondition<>([
            member1.id,
            member2.id,
        ] as Set))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage*.name =~ [
            "scope of member 1 & 2",
            "scope of member 2"
        ]
        result.totalResults == 2
    }

    def 'queries elements by scopes & composoites'() {
        given:
        def processInAComposite = processDataRepository.save(newProcess(unit) {
            name = "process in a composite"
        })
        def processInAScope = processDataRepository.save(newProcess(unit) {
            name = "process in a scope"
        })
        def processInAScopeAndComposite = processDataRepository.save(newProcess(unit) {
            name = "process in a scope and composite"
        })
        processDataRepository.save(newProcess(unit) {
            name = "first process in nothing"
        })
        processDataRepository.save(newProcess(unit) {
            name = "second process in nothing"
        })
        processDataRepository.save(newProcess(unit) {
            name = "process with two parts"
            parts = [
                processInAComposite,
                processInAScopeAndComposite
            ]
        })
        scopeDataRepository.saveAll([
            newScope(unit) {
                members = [
                    processInAScope,
                    processInAScopeAndComposite
                ]
            },
        ])

        when:
        def processesWithParent = new ElementQueryImpl(processDataRepository, client).with{
            whereParentElementPresent(true)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        processesWithParent.resultPage*.name =~ [
            "process in a composite",
            "process in a scope",
            "process in a scope and composite",
        ]

        when:
        def processesWithoutParent = new ElementQueryImpl(processDataRepository, client).with{
            whereParentElementPresent(false)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        processesWithoutParent.resultPage*.name =~ [
            "first process in nothing",
            "second process in nothing",
            "process with two parts",
        ]
    }

    def 'queries members by scope ID'() {
        given:
        def processInScopeA = processDataRepository.save(newProcess(unit) {
            name = "process in scope A"
        })
        def assetInScopeA = assetDataRepository.save(newAsset(unit) {
            name = "asset in scope A"
        })
        def processInScopeB = processDataRepository.save(newProcess(unit) {
            name = "process in scope B"
        })
        def processInBothScopes = processDataRepository.save(newProcess(unit) {
            name = "process in both scopes"
        })
        processDataRepository.save(newProcess(unit) {
            name = "process in nothing"
        })
        def scopeA = scopeDataRepository.save(newScope(unit) {
            members = [
                processInScopeA,
                assetInScopeA,
                processInBothScopes
            ]
        })
        def scopeB = scopeDataRepository.save(newScope(unit) {
            members = [
                processInScopeB,
                processInBothScopes,
            ]
        })

        when:
        def processesInScopeA = new ElementQueryImpl(elementDataRepository, client).with{
            whereScopesContain(new SingleValueQueryCondition<Key<UUID>>(scopeA.id))
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        processesInScopeA.resultPage*.name =~ [
            "process in scope A",
            "asset in scope A",
            "process in both scopes"
        ]

        when:
        def processesInScopeB = new ElementQueryImpl(processDataRepository, client).with{
            whereScopesContain(new SingleValueQueryCondition<Key<UUID>>(scopeB.id))
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        processesInScopeB.resultPage*.name =~ [
            "process in scope B",
            "process in both scopes"
        ]
    }

    def 'queries scopes by scopes'() {
        given:
        def firstScopeInAScope = scopeDataRepository.save(newScope(unit) {
            name = "first scope in a scope"
        })
        def secondScopeInAScope = scopeDataRepository.save(newScope(unit) {
            name = "second scope in a scope"
        })
        scopeDataRepository.save(newScope(unit) {
            name = "first scope in no scopes"
            members = [
                firstScopeInAScope,
                secondScopeInAScope
            ]
        })
        scopeDataRepository.save(newScope(unit) {
            name = "second scope in no scopes"
        })

        when:
        def scopesWithParent = new ElementQueryImpl(scopeDataRepository, client).with{
            whereParentElementPresent(true)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        scopesWithParent.resultPage*.name =~ [
            "first scope in a scope",
            "second scope in a scope",
        ]

        when:
        def scopesWithoutParent = new ElementQueryImpl(scopeDataRepository, client).with{
            whereParentElementPresent(false)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        scopesWithoutParent.resultPage*.name =~ [
            "first scope in no scopes",
            "second scope in no scopes",
        ]
    }

    def 'queries scopes by member presence'() {
        given:
        def member1 = scopeDataRepository.save(newScope(unit) {
            name = "member one"
        })
        def member2 = scopeDataRepository.save(newScope(unit) {
            name = "member two"
        })
        scopeDataRepository.saveAll([
            newScope(unit) {
                name = "scope with one member"
                members = [member1]
            },
            newScope(unit) {
                name = "scope with two members"
                members = [member1, member2]
            },
        ])

        when:
        def elementsWithChildren = new ElementQueryImpl<>(scopeDataRepository, client).with{
            whereChildElementsPresent(true)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        elementsWithChildren.resultPage*.name =~ [
            "scope with one member",
            "scope with two members"
        ]

        when:
        def elementsWithoutChildren = new ElementQueryImpl<>(scopeDataRepository, client).with {
            whereChildElementsPresent(false)
            execute(PagingConfiguration.UNPAGED)
        }

        then:
        elementsWithoutChildren.resultPage*.name =~ [
            "member one",
            "member two",
        ]
    }

    def 'queries all processes'() {
        given:
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "a"
                associateWithDomain(domain, "VT", "NEW")
            },
            newProcess(unit) {
                name = "b"
                associateWithDomain(domain, "VT", "NEW")
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

    def 'queries by status'() {
        given:
        def query = new ElementQueryImpl<>(processDataRepository, client)
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "a"
                associateWithDomain(domain, "VT", "GOOD")
            },
            newProcess(unit) {
                name = "b"
                associateWithDomain(domain, "VT", "OK")
            },
            newProcess(unit) {
                name = "c"
                associateWithDomain(domain, "VT", "BAD")
            },
            newProcess(unit) {
                name = "d"
            }
        ])

        when:
        query.whereStatusMatches(new QueryCondition<>(["GOOD", "OK"] as Set))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.totalResults == 2
        with(result.resultPage) {
            it.size() == 2
            it[0].name == "a"
            it[1].name == "b"
        }
    }

    def 'queries by sub type & status combined'() {
        given:
        def query = new ElementQueryImpl<>(processDataRepository, client)
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "a"
                associateWithDomain(domain, "VT", "GOOD")
            },
            newProcess(unit) {
                name = "b"
                associateWithDomain(domain, "VT", "BAD")
            },
            newProcess(unit) {
                name = "c"
                associateWithDomain(domain, "NT", "GOOD")
            },
            newProcess(unit) {
                name = "d"
                associateWithDomain(domain, "NT", "BAD")
            },
        ])

        when:
        query.whereSubTypeMatches(new QueryCondition<>(["VT"] as Set))
        query.whereStatusMatches(new QueryCondition<>(["GOOD"] as Set))
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.totalResults == 1
        with(result.resultPage) {
            it.size() == 1
            it[0].name == "a"
        }
    }

    def 'finds processes with no sub type'() {
        given:
        def query = new ElementQueryImpl<>(processDataRepository, client)
        processDataRepository.saveAll([
            newProcess(unit) {
                name = "a"
                associateWithDomain(domain, "VT", "NEW")
            },
            newProcess(unit) {
                name = "b"
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(processDataRepository, client)
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
        def query = new ElementQueryImpl<>(dataRepository, client)

        when:
        query.execute(new PagingConfiguration(2, 0, 'foo', SortOrder.ASCENDING))

        then:
        1 * dataRepository.findAll(_, { Pageable pageable->
            pageable.pageSize == 2
            pageable.pageNumber == 0
            pageable.sort.size() == 1
            pageable.sort.first().ascending
            pageable.sort.first().property == 'foo'
        }) >> Page.empty()
        1 * dataRepository.findAllWithDomainsLinksDecisionsByDbIdIn(_) >> []
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
        def query = new ElementQueryImpl<>(personDataRepository, client)

        def result = query.execute(new PagingConfiguration(100, 0, 'designator', SortOrder.ASCENDING))

        then: "the sort order is correct"
        with(result.resultPage) {
            it[0].name == "Person 1"
            it[22].name == "Person 23"
            it[41].name == "Person 42"
            it[99].name == "Person 100"
        }
    }

    def 'queries by domain'() {
        given:
        newDomain(client) { name = "one" }
        newDomain(client) { name = "two" }
        client = clientDataRepository.save(client)
        def domain1 = client.domains.find{it.name == "one"}
        def domain2 = client.domains.find{it.name == "two"}

        and:
        assetDataRepository.saveAll([
            newAsset(unit) {
                name = "one"
                associateWithDomain(domain1, "Application", "NEW")
            },
            newAsset(unit) {
                name = "two"
                associateWithDomain(domain1, "Application", "OLD")
                associateWithDomain(domain2, "App", "ARCHIVED")
            },
            newAsset(unit) {
                name = "three"
                associateWithDomain(domain2, "Application", "PLANNED")
            }
        ])

        def query = new ElementQueryImpl<>(assetDataRepository, client)

        when:
        query.whereDomainsContain(domain1)
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.resultPage.sort{it.name}*.name == ["one", "two"]
    }
}
