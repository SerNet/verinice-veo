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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Client
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
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
}
