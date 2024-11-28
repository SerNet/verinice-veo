/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import java.util.UUID;

import org.veo.core.entity.Identifiable;

/**
 * Thrown when an input entity contains a reference to an entity that could not be found.
 *
 * <p>When a requested entity could not be found, throw a {@link NotFoundException} instead.
 */
public class ReferenceTargetNotFoundException extends RuntimeException {

  public ReferenceTargetNotFoundException(String message) {
    super(message);
  }

  public ReferenceTargetNotFoundException(UUID id, Class<? extends Identifiable> targetType) {
    super("%s with ID %s not found".formatted(targetType.getName(), id));
  }
}
