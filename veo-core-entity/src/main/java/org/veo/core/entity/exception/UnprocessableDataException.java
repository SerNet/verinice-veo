/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
 * Invalid data is passed to an entity, most likely through a syntactically valid but semantically
 * invalid HTTP request
 */
public class UnprocessableDataException extends DomainException {

  private static final long serialVersionUID = 8076547359111532733L;

  public UnprocessableDataException(String string) {
    super(string);
  }
}
