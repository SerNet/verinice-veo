/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import org.veo.adapter.presenter.api.dto.full.FullScopeInDomainDto
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Client
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.base.UpdateElementInDomainUseCase
import org.veo.core.usecase.base.UpdateScopeInDomainUseCase
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository

@WithUserDetails("content-creator")
class UpdateScopeInDomainUseCaseITSpec extends VeoSpringSpec{
    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    ControlRepository controlRepository

    @Autowired
    ScopeRepository scopeRepository

    @Autowired
    EntityToDtoTransformer entityToDtoTransformer

    @Autowired
    UpdateScopeInDomainUseCase useCase

    Client client

    def setup() {
        client = createTestClient()
    }

    def "save a scope with control child and CI"() {
        given:
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID, false).tap{
            client.addToDomains(it)
        }
        def scope = executeInTransaction {
            def unit = unitRepository.save(newUnit(client))

            def control = controlRepository.save(
                    newControl(unit) {
                        associateWithDomain(domain, "Control", "hopeful")
                    }
                    )
            scopeRepository.save(newScope(unit) {
                associateWithDomain(domain, "SCP_Scope", "NEW")
                addMember(control)
                implementControl(control)
            })
        }

        when:
        FullScopeInDomainDto dto = entityToDtoTransformer.transformScope2Dto(scope, domain)

        def etag = ETag.from(scope)
        executeInTransaction {
            useCase.execute(new UpdateElementInDomainUseCase.InputData(scope.id, dto, domain.id, client, etag, null))
        }

        scope = executeInTransaction {
            scopeRepository.getById(scope.id, client.id).tap {
                it.controlImplementations.size()
                it.members.size()
            }
        }

        then:
        scope.controlImplementations.size() == 1
        scope.members.size() == 1
    }
}
