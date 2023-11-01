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
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.state.CompositeElementState
import org.veo.core.entity.state.CustomAspectState
import org.veo.core.entity.state.DomainAssociationState
import org.veo.core.usecase.base.ModifyElementUseCase.InputData
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.person.UpdatePersonUseCase
import org.veo.core.usecase.service.TypedId
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

import net.ttddyy.dsproxy.QueryCountHolder

class ModifyElementUseCaseITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UpdatePersonUseCase updatePersonUseCase

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", {
            -> true
        })
    }

    def "update a person"() {
        given:
        def client = createTestClient()
        def testDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def unit = unitDataRepository.save( newUnit(client).tap { unit->
            addToDomains(testDomain)
        })

        def person = personDataRepository.save(newPerson(unit))

        def updatedPerson = Mock(CompositeElementState) {
            name >> person.name
            abbreviation >> person.abbreviation
            description >> person.description
            it.owner >> TypedId.from(unit.idAsString, Unit)
            getDomainAssociationStates() >> [
                Mock(DomainAssociationState) {
                    getSubType() >> 'PER_Person'
                    getStatus() >> 'NEW'
                    getDomain() >> TypedId.from(testDomain.idAsString, Domain)
                    getCustomLinkStates() >> []
                    getCustomAspectStates() >> [
                        Mock(CustomAspectState) {
                            getType() >> 'person_address'
                            getAttributes() >> [:]
                        }
                    ]
                }
            ]
            getParts() >> []
        }

        QueryCountHolder.clear()
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when:
        executeInTransaction {
            updatePersonUseCase.execute(new InputData(person.idAsString, updatedPerson, unit.client, ETag.from(person.idAsString, 0), "user@domain.example"))
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:
        verifyAll {
            queryCounts.select == 12
            queryCounts.insert == 4
            queryCounts.update == 1
            queryCounts.delete == 0
            queryCounts.time < 1000
            // 20 is the currently observed count of 16 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 20
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
