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
import org.veo.core.entity.DomainException;
import org.veo.core.entity.ModelObject;

public class ClientBoundaryViolationException extends DomainException {

    public ClientBoundaryViolationException(ModelObject entity, Client unauthorizedClient) {
        super(String.format("The client boundary would be violated by the attempted operation on element: %s by client: %s",
                            entity.getId(), unauthorizedClient.getId()));
    }

}
