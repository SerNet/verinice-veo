/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import static java.util.UUID.randomUUID

import org.veo.core.entity.Client
import org.veo.core.entity.Unit

import spock.lang.Specification

public class GetUnitsUseCaseSpec extends Specification {

    Client existingClient
    Unit existingUnit

    def setup() {

        existingClient = Mock()
        existingClient.getId() >> UUID.randomUUID()
        existingClient.getDomains >> []
        existingClient.getName()>> "Existing client"

        existingUnit = Mock()
        existingUnit.getClient() >> existingClient
        existingUnit.getDomains() >> []
        existingUnit.getParent() >> null
        existingUnit.getName() >> "Existing unit"
        existingUnit.getId() >> randomUUID()

        existingClient.getUnits >> [existingUnit]
        existingClient.getUnit(_)>> Optional.of(existingUnit)

        existingClient.createUnit(_)>>existingUnit
    }
}
