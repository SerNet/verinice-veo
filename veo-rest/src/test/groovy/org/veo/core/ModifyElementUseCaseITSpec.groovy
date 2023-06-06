/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.core.entity.Client
import org.veo.core.usecase.base.ModifyElementUseCase.InputData
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.person.UpdatePersonUseCase
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

import net.ttddyy.dsproxy.QueryCountHolder

class ModifyElementUseCaseITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private CreateDemoUnitUseCase createDemoUnitUseCase

    @Autowired
    private UpdatePersonUseCase updatePersonUseCase

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

    def "update a person"() {
        given:
        def client = createTestClient()
        def testDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def unit = unitDataRepository.save( newUnit(client).tap { unit->
            addToDomains(testDomain)
        })

        def person = personDataRepository.save(newPerson(unit))
        def updatedPerson = newPerson(unit) {
            id = person.id
            associateWithDomain(testDomain, "PER_Person", "NEW")
            applyCustomAspect(newCustomAspect("person_address", testDomain))
        }

        QueryCountHolder.clear()
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when:
        executeInTransaction {
            updatePersonUseCase.execute(new InputData(updatedPerson, unit.client,ETag.from(person.idAsString, 0), "user@domain.example"))
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:
        verifyAll {
            queryCounts.select == 6
            queryCounts.insert == 4
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 1000
            // 10 is the currently observed count of 4 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 10
        }
    }

    Client createClient() {
        executeInTransaction {
            def client = newClient()
            defaultDomainCreator.addDefaultDomains(client)
            return clientRepository.save(client)
        }
    }
}
