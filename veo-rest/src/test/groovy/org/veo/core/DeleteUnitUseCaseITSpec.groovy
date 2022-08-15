/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class DeleteUnitUseCaseITSpec extends AbstractPerformaceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private CreateDemoUnitUseCase createDemoUnitUseCase

    @Autowired
    private DeleteUnitUseCase deleteUnitUseCase

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

    def "delete a demo unit"() {
        given: 'a client with a demo unit'
        def client = createTestClient()
        createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def demoUnit = createDemoUnit(client)
        QueryCountHolder.clear()
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead
        when: 'executing the DeleteUnitUseCase'
        def unit = runUseCase(demoUnit)
        def queryCounts = QueryCountHolder.grandTotal

        then: 'query statistics show sensible data'
        verifyAll {
            queryCounts.select == 91
            queryCounts.insert == 5
            queryCounts.update == 2
            queryCounts.delete in [39l, 40l]
            queryCounts.time < 1000
            // 200 is the currently observed count of 174 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 200
        }
    }

    def runUseCase(Unit unit) {
        executeInTransaction {
            deleteUnitUseCase.execute(new InputData(unit.id, unit.client))
        }
    }

    Client createClient() {
        executeInTransaction {
            def client = newClient()
            defaultDomainCreator.addDefaultDomains(client)
            return clientRepository.save(client)
        }
    }

    Unit createDemoUnit(Client client) {
        def unit = txTemplate.execute {
            createDemoUnitUseCase.execute(new CreateDemoUnitUseCase.InputData(client.id)).unit
        }
        unit
    }
}