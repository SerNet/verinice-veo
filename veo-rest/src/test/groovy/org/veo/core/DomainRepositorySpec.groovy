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
import org.veo.core.entity.condition.Condition
import org.veo.core.entity.condition.GreaterThanMatcher
import org.veo.core.entity.condition.PartCountExpression
import org.veo.core.entity.exception.NotFoundException
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.entity.jpa.DomainData

import jakarta.persistence.EntityManager
import jakarta.validation.ConstraintViolationException

class DomainRepositorySpec extends VeoSpringSpec {

    @Autowired
    private EntityManager em;

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

    def "domains can be deleted"() {
        given: "a domain"
        def client = createTestClient()
        Domain domain = newDomain(client) {
            name = "27001"
            authority = "me"
            translations: [
                de: [
                    name: "DS-GVO / BDSG / was auch immer",
                    description: "Besteste Domäne!",
                    abbreviation: "DBWAI"

                ]
            ]
            riskDefinitions = ["rd": createRiskDefinition("rd")]
            decisions.put("isGroup", newDecision(org.veo.core.entity.ElementType.CONTROL, "CTL_TOM") {
                rules.add(newRule(true) {
                    conditions.add(new Condition(new PartCountExpression("CTL_TOM"), new GreaterThanMatcher(BigDecimal.ZERO)))
                })
                defaultResultValue = false
            })

            applyInspection(name, newInspection())
        }
        domain = domainRepository.save(domain)
        def rdSetId = domain.riskDefinitionSet.id
        def deSetId = domain.decisionSet.id
        def inSetId = domain.inspectionSet.id

        when: "the domain is deleted"
        client.removeFromDomains(domain);
        domainRepository.deleteById(domain.getId());

        then: "no exception is raised"
        noExceptionThrown()

        and: "the domain and its related objects are gone"
        domainRepository.findById(domain.getId()).empty

        when: "checking the parts"
        def riskDefinitions = txTemplate.execute {
            em.createNativeQuery("select * from risk_definition_set where id=?1")
                    .setParameter(1, rdSetId).getResultList()
        }
        def decisions = txTemplate.execute {
            em.createNativeQuery("select * from decision_set where id=?1")
                    .setParameter(1, deSetId).getResultList()
        }
        def inspections = txTemplate.execute {
            em.createNativeQuery("select * from inspection_set where id=?1")
                    .setParameter(1, inSetId).getResultList()
        }

        then: "they are all gone"
        riskDefinitions.empty
        decisions.empty
        inspections.empty
    }
}
