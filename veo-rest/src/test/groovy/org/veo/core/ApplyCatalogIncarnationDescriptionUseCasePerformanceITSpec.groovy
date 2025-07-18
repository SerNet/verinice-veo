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

import java.util.concurrent.CompletableFuture
import java.util.function.Function

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.response.IncarnateDescriptionsDto
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.IncarnationLookup
import org.veo.core.entity.IncarnationRequestModeType
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.repository.RepositoryProvider
import org.veo.core.usecase.UseCaseInteractor
import org.veo.core.usecase.catalogitem.ApplyCatalogIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetCatalogIncarnationDescriptionUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class ApplyCatalogIncarnationDescriptionUseCasePerformanceITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private StoredEventDataRepository storedEventRepository

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

    private UseCaseInteractor synchronousUseCaseInteractor = [
        execute: {useCase, input, outputMapper->
            CompletableFuture.completedFuture(useCase.executeAndTransformResult(input,
                    outputMapper))
        }
    ] as UseCaseInteractor

    @Autowired
    private GetCatalogIncarnationDescriptionUseCase getIncarnationDescriptionUseCase

    @Autowired
    private ApplyCatalogIncarnationDescriptionUseCase applyIncarnationDescriptionUseCase

    @Autowired
    private RepositoryProvider repositoryProvider

    @Autowired
    private ReferenceAssembler urlAssembler

    private Client client
    private Unit unit

    def "SQL performance for getting and applying an incarnation description"() {
        given:
        createClient()
        Domain domain = createCatalogItems()
        QueryCountHolder.clear()
        def itemIds = domain.catalogItems.collect { it.symbolicId }
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when: "simulating the GET"
        def dto = executeInTransaction {
            def out = synchronousUseCaseInteractor.execute(
                    getIncarnationDescriptionUseCase,
                    new GetCatalogIncarnationDescriptionUseCase.InputData(client, unit.id, domain.id, itemIds, IncarnationRequestModeType.MANUAL, IncarnationLookup.FOR_REFERENCED_ITEMS, null, null),
                    Function.identity()
                    ).get()
            new IncarnateDescriptionsDto(out.references, urlAssembler)
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:
        dto.parameters.size() == 6
        queryCounts.select == 4
        // 12 is the currently observed count of 9 rows plus an acceptable safety margin
        DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 12

        when: "simulating the POST"
        QueryCountHolder.clear()
        rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead
        executeInTransaction {
            var references = dto.getParameters()
            synchronousUseCaseInteractor.execute(
                    applyIncarnationDescriptionUseCase,
                    new  ApplyCatalogIncarnationDescriptionUseCase.InputData(client, unit.id, references),
                    Function.identity()
                    ).get()
        }
        queryCounts = QueryCountHolder.grandTotal

        then:
        queryCounts.select == 11
        queryCounts.insert == 6
        queryCounts.time < 500
        // 30 is the currently observed count of 27 rows plus an acceptable safety margin
        DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 30
    }

    Client createClient() {
        executeInTransaction {
            client = newClient()
            def domain = newDomain(client) {
                name = "domain1"
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                    subTypes = [
                        Test: newSubTypeDefinition {
                        }
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.CONTROL, it) {
                    subTypes = [
                        Test: newSubTypeDefinition {
                        }
                    ]
                    links = [
                        externallinktest: newLinkDefinition(ElementType.CONTROL, "Test")
                    ]
                })
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.PROCESS, it) {
                    subTypes = [
                        Test: newSubTypeDefinition {
                        }
                    ]
                    links = [
                        aLink: newLinkDefinition(ElementType.CONTROL, "Test")
                    ]
                })
            }
            client = clientRepository.save(client)
            unit = newUnit(client) {
                addToDomains(domain)
            }
            unit = unitRepository.save(unit)
            client
        }
    }

    Domain createCatalogItems() {
        executeInTransaction {
            def domain = client.domains.first()
            newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                subType = "Test"
                status = "NEW"
                name = 'c1'
                abbreviation = 'c1'
                description = 'control number one'
            })

            CatalogItem item2 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                name = 'c2'
                subType = "Test"
                status = "NEW"
            })

            CatalogItem item3 = newCatalogItem(domain, {
                elementType = ElementType.CONTROL
                name = 'c3'
                subType = "Test"
                status = "NEW"
            })

            newTailoringReference(item3, item2, TailoringReferenceType.COPY)

            newCatalogItem(domain, {
                elementType = ElementType.ASSET
                name = 'd1'
                subType = "Test"
                status = "NEW"
            })

            CatalogItem item5 = newCatalogItem(domain, {
                elementType = ElementType.PROCESS
                name = 'p1'
                subType = "Test"
                status = "NEW"
            })

            newTailoringReference(item5, item2, TailoringReferenceType.COPY)

            newLinkTailoringReference(item5, item3, TailoringReferenceType.LINK) {
                linkType = "aLink"
            }

            CatalogItem item6 = newCatalogItem(domain,{
                elementType = ElementType.CONTROL
                name = 'c-p'
                subType = "Test"
                status = "NEW"
            })

            newLinkTailoringReference(item6, item2, TailoringReferenceType.LINK_EXTERNAL) {
                linkType = 'externallinktest'
            }

            domainDataRepository.save(domain)
        }
    }
}