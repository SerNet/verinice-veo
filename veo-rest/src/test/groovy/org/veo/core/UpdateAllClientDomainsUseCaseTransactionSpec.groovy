/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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

import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.usecase.MigrationFailedException
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase.InputData
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.security.NoRestrictionAccessRight

@WithUserDetails("user@domain.example")
class UpdateAllClientDomainsUseCaseTransactionSpec extends VeoSpringSpec {

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private UpdateAllClientDomainsUseCase useCase

    @SpringBean
    Decider deciderMock = Mock()

    Client client
    Client client2
    Domain dsgvoDomain
    Domain dsgvoDomainV2

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_V2_UUID)
        executeInTransaction {
            client = newClient()
            dsgvoDomain = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
            dsgvoDomainV2 = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
            client.addToDomains(dsgvoDomain)
            client.addToDomains(dsgvoDomainV2)
            client = clientRepository.save(client)
            dsgvoDomain = client.getDomains().find { it.domainTemplate.id == DSGVO_DOMAINTEMPLATE_UUID }
            dsgvoDomainV2 = client.getDomains().find { it.domainTemplate.id == DSGVO_DOMAINTEMPLATE_V2_UUID }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    def "Migrates units in separate transactions"() {
        given: "two units using the old domain"
        def unit1 = unitRepository.save(newUnit(client) {
            domains = [dsgvoDomain]
        })
        def unit2 = unitRepository.save(newUnit(client) {
            domains = [dsgvoDomain]
        })

        and: "an element in unit 2 that cannot be migrated"
        def unit2scenario = scenarioDataRepository.save(newScenario(unit2) {
            associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
        })

        deciderMock.decide(unit2scenario,dsgvoDomainV2) >> {
            throw new MigrationFailedException("failed", 1, 2)
        }

        when:
        executeInTransaction {
            useCase.execute(new InputData(DSGVO_DOMAINTEMPLATE_V2_UUID), NoRestrictionAccessRight.from(client.idAsString))
        }

        then:
        def ex = thrown(Exception)
        ex.message == "Migration failed for 1 of 1 client(s)"

        then: "unit 1 has been migrated"
        executeInTransaction { unitRepository.getById(unit1.id).domains*.id } ==~ [dsgvoDomainV2.id]

        and: "unit 2 has not been migrated"
        executeInTransaction { unitRepository.getById(unit2.id).domains*.id } ==~ [dsgvoDomain.id]
    }
}