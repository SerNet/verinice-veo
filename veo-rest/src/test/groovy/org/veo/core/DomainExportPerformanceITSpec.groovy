/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
 */
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.usecase.UseCase
import org.veo.core.usecase.domain.ExportDomainUseCase
import org.veo.rest.security.NoRestrictionAccessRight

import net.ttddyy.dsproxy.QueryCountHolder
import tools.jackson.databind.json.JsonMapper

@WithUserDetails("user@domain.example")
class DomainExportPerformanceITSpec extends AbstractPerformanceITSpec {

    @Autowired
    ExportDomainUseCase exportUseCase

    @Autowired
    EntityToDtoTransformer exportTransformer

    @Autowired
    JsonMapper exportJsonMapper

    def "a domain export is complete, needs the same statements for more profiles and loads nothing lazily"() {
        given: "a domain created from a template, with one profile"
        def client = createTestClient()
        def domainId = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID).id
        addProfile(domainId, 1)

        when: "exporting it, adding four more profiles and exporting it again"
        def first = exportDomain(client.id, domainId)
        (2..5).each { addProfile(domainId, it) }
        def second = exportDomain(client.id, domainId)

        then: "everything is exported"
        first.dto.profilesNew.size() == 1
        second.dto.profilesNew.size() == 5
        second.dto.catalogItems.size() > 0
        second.dto.profilesNew.every {
            it.items.size() == second.dto.catalogItems.size()
        }
        second.dto.profilesNew.every { profile ->
            profile.items.every {
                it.appliedCatalogItem != null && it.tailoringReferenceStates.size() == 1
            }
        }
        !second.json.empty

        and: "more profiles do not need more statements"
        second.loadSelects == first.loadSelects

        and: "nothing is loaded lazily while creating the DTO and writing the JSON"
        first.transformSelects == 0
        second.transformSelects == 0
        second.serializeSelects == 0
    }

    /**
     * Runs the export like the controller does: the use case loads the domain and the DTO is created
     * from it inside the transaction, the JSON is written outside of it.
     */
    private Map exportDomain(UUID clientId, UUID domainId) {
        Map result = [:]
        executeInTransaction {
            QueryCountHolder.clear()
            def output = exportUseCase.execute(
                    new UseCase.EntityId(domainId),
                    new NoRestrictionAccessRight(clientId, 10, 10))
            result.loadSelects = QueryCountHolder.grandTotal.select

            QueryCountHolder.clear()
            result.dto = exportTransformer.transformDomain2ExportDto(output.exportDomain())
            result.transformSelects = QueryCountHolder.grandTotal.select
        }

        QueryCountHolder.clear()
        result.json = exportJsonMapper.writeValueAsString(result.dto)
        result.serializeSelects = QueryCountHolder.grandTotal.select
        result
    }

    /** Adds a profile whose items apply all catalog items of the domain and link to each other. */
    private void addProfile(UUID domainId, int number) {
        executeInTransaction {
            def domain = domainDataRepository.findById(domainId).orElseThrow()
            def profile = newProfile(domain) {
                name = "Profile ${number}"
            }
            def items = domain.catalogItems.collect { catalogItem ->
                newProfileItem(profile) {
                    name = catalogItem.name
                    elementType = catalogItem.elementType
                    subType = catalogItem.subType
                    status = catalogItem.status
                    appliedCatalogItem = catalogItem
                }
            }
            items.eachWithIndex { item, int i ->
                item.addLinkTailoringReference(
                        TailoringReferenceType.LINK, items[(i + 1) % items.size()], "next", [:])
            }
        }
    }
}
