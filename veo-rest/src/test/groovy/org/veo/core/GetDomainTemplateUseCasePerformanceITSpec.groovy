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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.ElementType
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskMethod
import org.veo.core.repository.DomainTemplateRepository
import org.veo.core.repository.RepositoryProvider
import org.veo.core.service.UserAccessRightsProvider
import org.veo.core.usecase.UseCase
import org.veo.core.usecase.UseCaseInteractor
import org.veo.core.usecase.domaintemplate.GetDomainTemplateUseCase
import org.veo.jobs.UserSwitcher

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("content-creator")
class GetDomainTemplateUseCasePerformanceITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private GetDomainTemplateUseCase useCase

    @Autowired
    private EntityToDtoTransformer entityToDtoTransformer

    @Autowired
    private UserAccessRightsProvider userAccessRightsProvider

    private UseCaseInteractor synchronousUseCaseInteractor = [
        execute: {useCase, input, outputMapper->
            CompletableFuture.completedFuture(useCase.executeAndTransformResult(input,
                    outputMapper, userAccessRightsProvider.accessRights))
        }
    ] as UseCaseInteractor

    @Autowired
    private DomainTemplateRepository domainTemplateRepository

    @Autowired
    private RepositoryProvider repositoryProvider

    @Autowired
    private ReferenceAssembler urlAssembler

    def "SQL performance for exporting a domain template"() {
        given: "a domain template with a risk def, linked catalog items and linked profile items"
        def client = clientRepository.save(newClient {})
        def domainTemplateId = domainTemplateDataRepository.save(newDomainTemplate { dt ->
            dt.riskDefinitions.someDef = newRiskDefinition("someDef") {
                riskMethod = new RiskMethod()
                categories = [
                    newCategoryDefinition("nianCat")
                ]
                probability.levels = [
                    new ProbabilityLevel()
                ]
            }
            50.times {
                dt.catalogItems.add(newCatalogItem(dt) {
                    it.elementType = ElementType.CONTROL
                    it.status = "BRAND_NEW"
                    it.subType = "RemoteControl"
                })
            }
            dt.catalogItems.each {
                it.addTailoringReference(TailoringReferenceType.COMPOSITE, it)
                it.addTailoringReference(TailoringReferenceType.PART, it)
            }
            5.times {
                dt.profiles.add(newProfile(dt) { profile ->
                    30.times {
                        profile.items.add(newProfileItem(profile) {
                            it.elementType = ElementType.CONTROL
                            it.status = "BRAND_NEW"
                            it.subType = "RemoteControl"
                            it.appliedCatalogItem = dt.catalogItems[0]
                            it.addTailoringReference(TailoringReferenceType.COMPOSITE, it)
                            it.addTailoringReference(TailoringReferenceType.PART, it)
                        })
                    }
                })
            }
        }).id
        QueryCountHolder.clear()

        when: "simulating the GET"
        new UserSwitcher().runAsUser("user", client.idAsString) {
            executeInTransaction {
                synchronousUseCaseInteractor.execute(
                        useCase,
                        new UseCase.EntityId(domainTemplateId), {
                            entityToDtoTransformer.transformDomainTemplate2Dto(it.domainTemplate)
                        }
                        ).get()
            }
        }
        def queryCounts = QueryCountHolder.grandTotal

        then:
        queryCounts.select == 13
    }
}