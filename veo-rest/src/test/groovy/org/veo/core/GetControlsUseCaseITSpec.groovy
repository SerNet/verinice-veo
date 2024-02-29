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
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.repository.PagingConfiguration
import org.veo.core.usecase.base.GetElementsUseCase
import org.veo.core.usecase.control.GetControlsUseCase
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class GetControlsUseCaseITSpec extends AbstractPerformanceITSpec {
    Client client
    Unit unit
    Domain testDomain
    Domain dsgvoDomain

    @Autowired
    GetControlsUseCase getControlsUseCase

    @Autowired EntityToDtoTransformer entityToDtoTransformer

    def setup() {
        client = createTestClient()
        testDomain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
        dsgvoDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.save(client)

        unit = unitDataRepository.save(newUnit(client) {
            domains = [testDomain, dsgvoDomain]
        })
    }

    def "Sensible query counts for composite elements"() {
        given:
        def numberOfControlsToCreate = 100
        def numberOfControlsToRetrieve = 50
        executeInTransaction {
            def controls = (1..numberOfControlsToCreate).collect {idx->
                newControl(unit) {
                    it.name = "Cotrol $idx"
                }
            }
            controlDataRepository.saveAll(controls)
        }

        QueryCountHolder.clear()

        when:
        def output = executeInTransaction {
            getControlsUseCase.execute(new GetElementsUseCase.InputData(client, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, new PagingConfiguration(numberOfControlsToRetrieve, 0, 'name', PagingConfiguration.SortOrder.ASCENDING))).tap {
                elements.resultPage.each {
                    entityToDtoTransformer.transform2Dto(it, false)
                }
            }
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:
        output.elements.totalResults == numberOfControlsToCreate
        verifyAll {
            queryCounts.select == 8
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
        }
    }
}
