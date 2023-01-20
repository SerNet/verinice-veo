/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.entity.definitions.attribute;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.time.format.DateTimeParseException;

import org.veo.core.entity.exception.InvalidAttributeException;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DateAttributeDefinition extends AttributeDefinition {
  public static final String TYPE = "date";

  @Override
  public void validate(Object value) throws InvalidAttributeException {
    if (value instanceof String str) {
      try {
        ISO_LOCAL_DATE.parse(str);
      } catch (DateTimeParseException ex) {
        throw new InvalidAttributeException("must be a date in 'yyyy-MM-dd' format");
      }
    } else throw new InvalidAttributeException("must be a string");
  }
}
