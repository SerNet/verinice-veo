/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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
package org.veo.core.usecase.catalogitem

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.CatalogItemRepository
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.ElementQuery
import org.veo.core.repository.ElementRepository
import org.veo.core.repository.GenericElementRepository
import org.veo.core.repository.PagedResult
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec

abstract class ApplyIncarnationDescriptionSpec extends UseCaseSpec {
    DesignatorService designatorService = Mock()
    CatalogItemRepository catalogItemRepository = Mock()
    DomainRepository domainRepository = Mock()
    GenericElementRepository genericElementRepository = Mock()
    ElementRepository repo = Mock()
    UnitRepository unitRepo = Mock()
    CatalogItem item1 = Spy()
    CatalogItem item2 = Spy()
    Control control = Mock()
    Control newControl = Mock()
    ElementQuery emptyQuery = Mock()
    PagedResult emptyQueryResult = Mock()

    def setup() {
        // state the obvious
        unitRepo.getByIdFetchClient(existingUnit.id) >> existingUnit
        unitRepo.getByIdFetchClient(_) >> { throw new NotFoundException("") }
        unitRepo.getById(_) >> { throw new NotFoundException("") }

        newControl.getModelInterface() >> Control.class

        //and some basic behavior
        emptyQuery.execute(_) >> emptyQueryResult
        emptyQueryResult.getResultPage() >> []

        def existingDomainId = UUID.randomUUID()
        existingDomain.id >> existingDomainId
        existingDomain.id >> existingDomainId
        existingDomain.owner >> existingClient
        existingDomain.modelInterface >> Domain.class
        domainRepository.getById(existingDomainId) >> existingDomain
        domainRepository.getActiveById(existingDomainId, existingClient.id) >> existingDomain

        item1.symbolicId >> UUID.randomUUID()
        item1.domainBase >> existingDomain
        item1.requireDomainMembership() >> existingDomain
    }
}
