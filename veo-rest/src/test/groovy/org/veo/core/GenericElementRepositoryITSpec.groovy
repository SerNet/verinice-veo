/*
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
 */
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Client
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.repository.GenericElementRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DocumentRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import jakarta.transaction.Transactional

@Transactional()
class GenericElementRepositoryITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private PersonRepositoryImpl personRepository
    @Autowired
    private DocumentRepositoryImpl documentRepository
    @Autowired
    private ScopeRepositoryImpl scopeRepository

    @Autowired
    private GenericElementRepository genericElementRepository

    private Client client
    private Unit unit

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(this.client))
    }

    def "Element counts can be queried"() {
        given:
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        executeInTransaction{
            2.times {
                personRepository.save(newPerson(unit) {
                    associateWithDomain(domain, 'PER_Person', 'NEW')
                })
            }
            personRepository.save( newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'IN_PROGRESS')
            })
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_DataProtectionOfficer', 'RELEASED')
            })
            documentRepository.save(newDocument(unit) {
                associateWithDomain(domain, 'DOC_Contract', 'ARCHIVED')
            })
            scopeRepository.save(newScope(unit) {
                associateWithDomain(domain, 'SCP_ResponsibleBody', 'NEW')
            })
        }

        when:
        def counts = genericElementRepository.getCountsBySubType(unit, domain).toSorted { a,b ->
            a.type <=> b.type ?: a.subType <=> b.subType ?: a.status <=> b.status
        }

        then:
        counts.size() == 5
        with(counts[0]) {
            type == ElementType.DOCUMENT
            subType == 'DOC_Contract'
            status == 'ARCHIVED'
            count == 1
        }
        with(counts[1]) {
            type == ElementType.PERSON
            subType == 'PER_DataProtectionOfficer'
            status == 'RELEASED'
            count == 1
        }
        with(counts[2]) {
            type == ElementType.PERSON
            subType == 'PER_Person'
            status == 'IN_PROGRESS'
            count == 1
        }
        with(counts[3]) {
            type == ElementType.PERSON
            subType == 'PER_Person'
            status == 'NEW'
            count == 2
        }
        with(counts[4]) {
            type == ElementType.SCOPE
            subType == 'SCP_ResponsibleBody'
            status == 'NEW'
            count == 1
        }
    }

    def "finds custom aspect attribute values filtered by type and key"() {
        given:
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        executeInTransaction {
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'PT8H'
                    }
                ]
            })
        }

        when:
        def values = genericElementRepository.findUsedAttributeValues(
                unit.id, domain.id, ['person_duration': ['estimatedDuration'] as Set], [:], DurationAttributeDefinition.TYPE, 100)

        then:
        values == ['PT8H']

        and: "an unrelated type yields nothing"
        genericElementRepository.findUsedAttributeValues(
                unit.id, domain.id, ['nope': ['x'] as Set], [:], '', 100).isEmpty()
    }

    def "finds custom link attribute values filtered by type and key"() {
        given:
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def target = personRepository.save(newPerson(unit) { associateWithDomain(domain, 'PER_Person', 'NEW') })
        executeInTransaction {
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                applyLink(newCustomLink(target, 'person_link', domain) { attributes['recoveryTime'] = 'P3D' })
            })
        }

        when:
        def values = genericElementRepository.findUsedAttributeValues(
                unit.id, domain.id, [:], ['person_link': ['recoveryTime'] as Set], DurationAttributeDefinition.TYPE, 100)

        then:
        values == ['P3D']
    }

    def "database orders all duration period values correctly"() {
        given: "a domain"
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        and: "an unsorted set covering years, months, weeks, days, hours, minutes, seconds, combined values, zero and duplicates"
        def savedDurations = [
            "PT1S",
            "P1D",
            "PT59S",
            "P1Y",
            "PT1M",
            "P1M",
            "PT1H",
            "P2W",
            "P0D",
            "P2Y",
            "PT4M",
            "P3D",
            "P2D",
            "P6M",
            "PT2S",
            "P21D",
            "P1Y2M3DT4H5M6S",
            "P6M15DT12H",
            "P3W2D",
            "P1DT1H",
            "PT1H30M",
            "PT1M30S",
            "PT59M",
            "PT23H",
            "P6D",
            "P3M",
            "P7D",
            // duplicate, should be returned only once
            "P1D"
        ]

        executeInTransaction {
            savedDurations.each { duration ->
                personRepository.save(newPerson(unit) {
                    associateWithDomain(domain, 'PER_Person', 'NEW')

                    customAspects = [
                        newCustomAspect('person_duration', domain) {
                            attributes['estimatedDuration'] = duration
                        }
                    ]
                })
            }

            // Requested key, but wrong custom aspect type.
            // This should not be returned because the type is not queried.
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')

                customAspects = [
                    newCustomAspect('other_duration_type', domain) {
                        attributes['estimatedDuration'] = 'P998D'
                    }
                ]
            })
        }

        when: "all used attribute values are queried"
        def values = genericElementRepository.findUsedAttributeValues(
                unit.id,
                domain.id,
                ['person_duration': ['estimatedDuration'] as Set],
                [:],
                DurationAttributeDefinition.TYPE,
                100
                )

        and: "values are converted to plain strings for assertion"
        def valueStrings = values.collect { it?.toString() }

        then: "duplicates are removed and ignored values are not returned"
        values.size() == 27
        !valueStrings.contains('P998D')

        and: "values are deserialized, not returned as quoted JSON strings"
        valueStrings.every { it != null && !it.startsWith('"') }

        and: "all period types are sorted ascending by interval"
        valueStrings == [
            "P0D",
            "PT1S",
            "PT2S",
            "PT59S",
            "PT1M",
            "PT1M30S",
            "PT4M",
            "PT59M",
            "PT1H",
            "PT1H30M",
            "PT23H",
            "P1D",
            "P1DT1H",
            "P2D",
            "P3D",
            "P6D",
            "P7D",
            "P2W",
            "P21D",
            "P3W2D",
            "P1M",
            "P3M",
            "P6M",
            "P6M15DT12H",
            "P1Y",
            "P1Y2M3DT4H5M6S",
            "P2Y"
        ]

        when: "the query is limited"
        def top5 = genericElementRepository.findUsedAttributeValues(
                unit.id,
                domain.id,
                ['person_duration': ['estimatedDuration'] as Set],
                [:],
                DurationAttributeDefinition.TYPE,
                5
                )

        and:
        def top5Strings = top5.collect { it?.toString() }

        then: "the limit respects the sorted order"
        top5.size() == 5
        top5Strings == [
            "P0D",
            "PT1S",
            "PT2S",
            "PT59S",
            "PT1M"
        ]
    }

    def "generic query returns non-duration values without interval sorting"() {
        given: "a domain"
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        and: "non-duration values, including values that look like durations"
        def savedValues = [
            "ZEBRA",
            "ACTIVE",
            "P3D",
            "PT1S",
            "foo",
            // duplicate, should be returned only once
            "ACTIVE"
        ]

        executeInTransaction {
            savedValues.each { text ->
                personRepository.save(newPerson(unit) {
                    associateWithDomain(domain, 'PER_Person', 'NEW')

                    customAspects = [
                        newCustomAspect('person_text', domain) {
                            attributes['name'] = text
                        }
                    ]
                })
            }

            // Wrong key, should not be returned.
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')

                customAspects = [
                    newCustomAspect('person_text', domain) {
                        attributes['otherName'] = 'P997D'
                    }
                ]
            })

            // Wrong aspect type, should not be returned.
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')

                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'P999D'
                    }
                ]
            })
        }

        when: "non-duration attribute values are queried"
        def values = genericElementRepository.findUsedAttributeValues(
                unit.id,
                domain.id,
                ['person_text': ['name'] as Set],
                [:],
                TextAttributeDefinition.TYPE,
                100
                )

        and: "values are converted to plain strings for assertion"
        def valueStrings = values.collect { it?.toString() }

        then: "duplicates are removed and ignored values are not returned"
        values.size() == 5
        !valueStrings.contains('P997D')
        !valueStrings.contains('P999D')

        and: "values are deserialized, not returned as quoted JSON strings"
        valueStrings.every { it != null && !it.startsWith('"') }

        and: "values are sorted as plain text, not as intervals"
        valueStrings == [
            "ACTIVE",
            "P3D",
            "PT1S",
            "ZEBRA",
            "foo"
        ]

        when: "the query is limited"
        def top3 = genericElementRepository.findUsedAttributeValues(
                unit.id,
                domain.id,
                ['person_text': ['name'] as Set],
                [:],
                TextAttributeDefinition.TYPE,
                3
                )

        and:
        def top3Strings = top3.collect { it?.toString() }

        then: "the limit respects the generic sort order"
        top3.size() == 3
        top3Strings == [
            "ACTIVE",
            "P3D",
            "PT1S"
        ]
    }
}
