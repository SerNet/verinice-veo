/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.IncarnationConfiguration
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.entity.transform.IdentifiableFactory
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.PagedResult
import org.veo.core.repository.RepositoryProvider
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.base.UnitHierarchyProvider
import org.veo.core.usecase.service.RefResolverFactory

import spock.lang.Specification

/**
 * Base class for use-case unit tests
 */
abstract class UseCaseSpec extends Specification {

    Client existingClient
    Client anotherClient
    Unit existingUnit
    Domain existingDomain
    Set<Unit> existingUnitHierarchyMembers
    ClientRepository clientRepository = Mock()
    UnitHierarchyProvider unitHierarchyProvider = Mock()
    UnitRepository unitRepository = Mock()
    RepositoryProvider repositoryProvider = Mock()
    EntityFactory entityFactory = Mock()
    IdentifiableFactory identifiableFactory = Mock()
    RefResolverFactory refResolverFactory = new RefResolverFactory(repositoryProvider, identifiableFactory)

    def setup() {
        existingDomain = Mock()
        existingDomain.modelInterface >> Domain.class
        existingDomain.incarnationConfiguration >> new IncarnationConfiguration()

        def id1 = Key.newUuid()
        Client client = Mock()
        client.getId() >> id1
        client.getDomains() >> [existingDomain]
        existingDomain.getOwner() >> client
        client.getName()>> "Existing client"
        existingClient = client

        def id2 = Key.newUuid()
        anotherClient = Mock()
        anotherClient.getId() >> id2
        anotherClient.getName()>> "Another client"

        def id = Key.newUuid()

        existingUnit = Mock()
        existingUnit.getClient() >> client
        existingUnit.getDomains() >> []
        existingUnit.getParent() >> null
        existingUnit.getName() >> "Existing unit"
        existingUnit.getId() >> id
        existingUnit.getIdAsString() >> id.uuidValue()
        existingUnit.idAsUUID >> id.value()
        existingUnit.getModelInterface() >> Unit
        existingUnit.getVersion() >> 0

        existingUnitHierarchyMembers = [
            existingUnit,
            Mock(Unit),
            Mock(Unit)
        ]

        client.createUnit(_)>>existingUnit
    }

    PagedResult singleResult(entity, pagingConfiguration) {
        new PagedResult(pagingConfiguration,[entity],  1, 1)
    }
}
