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

import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.usecase.UseCase
import org.veo.core.usecase.domain.ExportDomainUseCase
import org.veo.rest.security.NoRestrictionAccessRight

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class ExportDomainQueryDBPerformanceSpec extends VeoSpringSpec {
    Client client
    Domain testDomain

    @Autowired
    ExportDomainUseCase useCase

    @Autowired
    EntityToDtoTransformer entityToDtoTransformer

    def setup() {
        client = createTestClient()
        testDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.save(client)
    }

    def "Export a domain with catalog items and profiles"() {
        when:
        QueryCountHolder.clear()

        def dto = executeInTransaction {
            def result = useCase.execute(new UseCase.EntityId(testDomain.id), NoRestrictionAccessRight.from(client.idAsString, 2, 2))

            def d = result.exportDomain()
            entityToDtoTransformer.transformDomain2ExportDto(d)
        }

        def queryCounts = QueryCountHolder.grandTotal

        then:
        dto.catalogItems.size() == 65
        dto.profilesNew.size() == 1
        with(dto.profilesNew.first()) {
            it.items.size() == 9
        }
        verifyAll {
            queryCounts.select == 6
            queryCounts.insert == 0
            queryCounts.update == 0
            queryCounts.delete == 0
            queryCounts.time < 1000
        }
    }
}
