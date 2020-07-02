/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.usecase;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.SameClientSpecification;

/**
 * Superclass for all use cases. Each use case must provide an implementation of
 * input and output data structures.
 *
 * @author akoderman
 *
 * @param <I>
 * @param <O>
 */
public abstract class UseCase<I, O> {

    public abstract O execute(I input);

    protected static void checkSameClient(Client authenticatedClient, EntityLayerSupertype entity) {
        Unit unit = entity.getOwner();
        checkSameClient(authenticatedClient, unit, entity);
    }

    // TODO VEO-124 this check should always be done implicitly by UnitImpl or
    // ModelValidator. Without this check, it would be possible to overwrite
    // objects from other clients with our own clientID, thereby hijacking these
    // objects!
    protected static void checkSameClient(Client authenticatedClient, Unit unit,
            ModelObject elementToBeModified) {
        Client client = unit.getClient();
        if (!(new SameClientSpecification<>(authenticatedClient).isSatisfiedBy(client))) {
            throw new ClientBoundaryViolationException("The client boundary would be "
                    + "violated by the attempted operation on element: "
                    + elementToBeModified.toString() + " from client "
                    + authenticatedClient.toString());
        }
    }
}