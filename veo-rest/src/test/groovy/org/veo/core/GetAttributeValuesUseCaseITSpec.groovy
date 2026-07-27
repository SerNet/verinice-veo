/*
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi.
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
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.GenericElementRepository
import org.veo.core.usecase.domain.GetAttributeValuesUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

class GetAttributeValuesUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    ClientRepositoryImpl clientRepository
    @Autowired
    UnitRepositoryImpl unitRepository
    @Autowired
    PersonRepositoryImpl personRepository
    @Autowired
    DomainRepository domainRepository
    @Autowired
    GenericElementRepository genericElementRepository
    @Autowired
    GetAttributeValuesUseCase getAttributeValuesUseCase

    Client client
    Unit unit
    Domain domain
    UserAccessRights user = Mock()

    def setup() {
        client = createTestClient()
        domain = newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                subTypes = [
                    PER_Person: newSubTypeDefinition {
                        sortKey = 1
                    }
                ]
                customAspects = [
                    person_duration: new CustomAspectDefinition().tap {
                        attributeDefinitions = [estimatedDuration: new DurationAttributeDefinition()]
                    }
                ]
                links = [
                    person_link: new LinkDefinition().tap {
                        targetType = ElementType.PERSON
                        targetSubType = "PER_Person"
                        attributeDefinitions = [recoveryTime: new DurationAttributeDefinition()]
                    }
                ]
            })
        }
        client = clientRepository.save(client)
        domain = client.domains.first()
        unit = unitRepository.save(newUnit(client))
        user.getClientId() >> client.id
    }

    private void saveDurationData() {
        def target = personRepository.save(newPerson(unit) { associateWithDomain(domain, 'PER_Person', 'NEW') })
        executeInTransaction {
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'PT8H'
                    }
                ]
            })
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'PT8H'
                    }
                ]
            })
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'P3D'
                    }
                ]
                applyLink(newCustomLink(target, 'person_link', domain) { attributes['recoveryTime'] = 'P1D' })
            })
        }
    }

    def "returns distinct duration values sorted descending across CA and link attributes"() {
        given:
        saveDurationData()

        when:
        def output = getAttributeValuesUseCase.execute(
                new GetAttributeValuesUseCase.InputData(domain.id, unit.id, 'duration'), user)

        then:
        output.values() == ['PT8H', 'P1D', 'P3D']
        !output.truncated()
    }

    def "caps the result set and flags truncation"() {
        given:
        saveDurationData()
        def cappedUseCase = new GetAttributeValuesUseCase(
                domainRepository, unitRepository, genericElementRepository, 2)

        when:
        def output = cappedUseCase.execute(
                new GetAttributeValuesUseCase.InputData(domain.id, unit.id, 'duration'), user)

        then:
        output.values() == ['PT8H', 'P1D']
        output.truncated()
    }

    def "rejects an unknown attribute type"() {
        when:
        getAttributeValuesUseCase.execute(
                new GetAttributeValuesUseCase.InputData(domain.id, unit.id, 'bogus'), user)

        then:
        thrown(IllegalArgumentException)
    }
}
