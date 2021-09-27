/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.entity.specification;

import org.veo.core.entity.Client;
import org.veo.core.entity.ClientOwned;

import lombok.RequiredArgsConstructor;

/**
 * Clients must be strictly separated. All references from one object to another
 * including composite relationships must be references to objects that belong
 * to the same client.
 *
 * Provides methods to check for equality of client and unit objects and
 * collections in addition to the entity checks from the interface
 * <code>IEntitySecification</code>.
 */
@RequiredArgsConstructor
public class SameClientSpecification implements EntitySpecification<ClientOwned> {

    private final Client client;

    @Override
    public boolean test(ClientOwned entity) {
        return isSatisfiedBy(entity.getOwningClient()
                                   .orElse(null));
    }

    public boolean isSatisfiedBy(Client otherClient) {
        return client.equals(otherClient);
    }
}
