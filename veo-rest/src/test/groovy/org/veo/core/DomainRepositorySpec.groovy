/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import org.veo.core.entity.Domain
import org.veo.core.entity.exception.NotFoundException
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.entity.jpa.DomainData

import jakarta.validation.ConstraintViolationException

class DomainRepositorySpec extends VeoSpringSpec {

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    private DomainDataRepository domainDataRepository

    def "cannot violate the composition association between Client and Domain"() {
        given: "a domain"
        Domain domain = new DomainData().tap{
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }

        when: "the domain is saved without client association"
        domainRepository.save(domain)

        then: "an exception is raised"
        thrown(ConstraintViolationException)

        and: "it was not saved"
        domainDataRepository.findAll().empty
    }

    def "getByIds returns domain for valid ID"() {
        given:
        def client = createTestClient()
        def domain = domainRepository.save(newDomain(client))

        when:
        def result = domainRepository.getByIds([domain.id] as Set, client.id)

        then:
        result.size() == 1
        result ==~ [domain]
    }

    def "getByIds throws for unknown ID"() {
        given:
        def client = createTestClient()
        def domain = domainRepository.save(newDomain(client))
        def randomId = UUID.randomUUID()

        when:
        domainRepository.getByIds([
            domain.id,
            randomId
        ] as Set, client.id)

        then:
        NotFoundException e = thrown()
        e.message == "Domain $randomId not found"
    }

    def "getByIds throws for other client's domain"() {
        given:
        def client = createTestClient()
        def otherClient = clientRepository.save(newClient())
        def domain = domainRepository.save(newDomain(otherClient))

        when:
        domainRepository.getByIds([
            domain.id,
        ] as Set, client.id)

        then:
        NotFoundException e = thrown()
        e.message == "Domain ${domain.idAsString} not found"
    }
}
