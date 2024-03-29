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

import org.veo.core.entity.Asset
import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Process
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.usecase.UseCaseInteractor
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase
import org.veo.persistence.access.CatalogRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.StoredEventDataRepository

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class ApplyIncarnationDescriptionUseCasePerformanceITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private StoredEventDataRepository storedEventRepository

    @Autowired
    private CatalogRepositoryImpl catalogRepository

    private UseCaseInteractor synchronousUseCaseInteractor = [
        execute: {useCase, input, outputMapper->
            CompletableFuture.completedFuture(useCase.executeAndTransformResult(input,
                    outputMapper))
        }
    ] as UseCaseInteractor

    @Autowired
    private GetIncarnationDescriptionUseCase getIncarnationDescriptionUseCase

    @Autowired
    private ApplyIncarnationDescriptionUseCase applyIncarnationDescriptionUseCase

    private Client client
    private Unit unit

    def "SQL performance for getting and applying an incarnation description"() {
        given:
        createClient()
        Catalog catalog = createCatalog()
        QueryCountHolder.clear()

        when:
        def inputDataGetIncarnationDescription = new GetIncarnationDescriptionUseCase.InputData(client, unit.id, catalog.catalogItems.collect{it.id})
        GetIncarnationDescriptionUseCase.OutputData description = executeInTransaction {
            synchronousUseCaseInteractor.execute(getIncarnationDescriptionUseCase, inputDataGetIncarnationDescription, Function.identity()).get()
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:
        description.references.size() == 8
        queryCounts.select == 7

        when:
        def inputData = new  ApplyIncarnationDescriptionUseCase.InputData(client, unit.id, description.references)
        QueryCountHolder.clear()
        executeInTransaction {
            synchronousUseCaseInteractor.execute(applyIncarnationDescriptionUseCase, inputData, Function.identity()).get()
        }
        queryCounts = QueryCountHolder.grandTotal

        then:
        queryCounts.select == 10
        queryCounts.insert == 22
        queryCounts.time < 500
    }

    Client createClient() {
        executeInTransaction {
            client = newClient()
            def domain = newDomain(client) {
                name = "domain1"
            }
            client = clientRepository.save(client)
            unit = newUnit(client) {
                addToDomains(domain)
            }
            unit = unitRepository.save(unit)
            client
        }
    }

    Catalog createCatalog() {
        executeInTransaction {
            def domain = client.domains.first()
            Catalog catalog = newCatalog(domain) {
                name = 'a'
            }

            CatalogItem item1 = newCatalogItem(catalog, {
                elementType = Control.SINGULAR_TERM
                subType = "Test"
                status = "NEW"
                name = 'c1'
                abbreviation = 'c1'
                description = 'control number one'
            })

            CatalogItem item2 = newCatalogItem(catalog, {
                elementType = Control.SINGULAR_TERM
                name = 'c2'
                subType = "Test"
                status = "NEW"
            })

            CatalogItem item3 = newCatalogItem(catalog, {
                elementType = Control.SINGULAR_TERM
                name = 'c3'
                subType = "Test"
                status = "NEW"
            })

            newTailoringReference(item3, TailoringReferenceType.COPY) {
                catalogItem = item2
            }

            CatalogItem item4 = newCatalogItem(catalog, {
                elementType = Asset.SINGULAR_TERM
                name = 'd1'
                subType = "Test"
                status = "NEW"
            })

            CatalogItem item5 = newCatalogItem(catalog, {
                elementType = Process.SINGULAR_TERM
                name = 'p1'
                subType = "Test"
                status = "NEW"
            })

            newTailoringReference(item5, TailoringReferenceType.COPY) {
                catalogItem = item2
            }

            newLinkTailoringReference(item5, TailoringReferenceType.LINK) {
                catalogItem = item3
                linkType = "aLink"
            }

            CatalogItem item6 = newCatalogItem(catalog,{
                elementType = Control.SINGULAR_TERM
                name = 'c-p'
                subType = "Test"
                status = "NEW"
            })

            newLinkTailoringReference(item6, TailoringReferenceType.LINK_EXTERNAL) {
                catalogItem = item2
                linkType = 'externallinktest'
            }

            catalog.catalogItems = [
                item1,
                item2,
                item3,
                item4,
                item5,
                item6
            ] as Set

            catalogRepository.save(catalog)
        }
    }
}