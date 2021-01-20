/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.core.entity.exception;

import org.veo.core.entity.DomainException;

/**
 * Runtime exception that is thrown when an attempted operation would violate
 * the consistency rules of the domain model.
 */
public class ModelConsistencyException extends DomainException {
    public ModelConsistencyException(String messageTemplate, Object... arguments) {
        super(String.format(messageTemplate, arguments));
    }
}
