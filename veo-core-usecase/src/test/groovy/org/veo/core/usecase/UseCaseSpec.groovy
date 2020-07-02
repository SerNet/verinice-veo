/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.entity.transform.TransformContextProvider
import org.veo.core.usecase.repository.ClientRepository
import org.veo.core.usecase.repository.RepositoryProvider
import org.veo.core.usecase.repository.UnitRepository
import org.veo.test.VeoSpec

/**
 * Base class for use-case unit tests
 */
abstract class UseCaseSpec extends VeoSpec {

    Client existingClient
    Unit existingUnit
    ClientRepository clientRepository = Mock()
    UnitRepository unitRepository = Mock()
    TransformContextProvider transformContextProvider = Mock()
    RepositoryProvider repositoryProvider = Mock()

    def setup() {
        existingClient = newClient{
            name = "Existing client"
        }
        existingUnit = existingClient.createUnit("Existing unit")
    }
}
