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
package org.veo.core.entity.exception;

import org.veo.core.entity.DomainException;

/**
 * Runtime exception thrown when a requested object could not be found, i.e.
 * during an update operation. This could be caused by concurrent editing of an
 * object - since the version number of the object has been changed between \
 * the beginning of a user's editing and the attempted save operation.
 *
 * This exception must be caught by the controller and an appropriate status
 * code and message must be returned to the caller. The caller will need to
 * remediate the situation, i.e. by temporarily noting down all changes, then
 * reloading the object and applying the changes again.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String messageTemplate, Object... arguments) {
        super(String.format(messageTemplate, arguments));
    }
}
