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

import java.net.URI;
import java.util.regex.Pattern;

import org.veo.core.entity.exception.InvalidAttributeException;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public non-sealed class ExternalDocumentAttributeDefinition extends AttributeDefinition {
  private static final String[] SUPPORTED_PROTOCOLS = {"http", "https", "ftp", "ftps", "smb"};
  public static final String PROTOCOL_PATTERN =
      "^(" + String.join("|", SUPPORTED_PROTOCOLS) + ")://";
  public static final String TYPE = "externalDocument";
  private static final Pattern PROTOCOL_PATTERN_COMPILED = Pattern.compile(PROTOCOL_PATTERN);

  @Override
  public void validate(Object value) throws InvalidAttributeException {
    if (value instanceof String str) {
      try {
        URI.create(str);
      } catch (IllegalArgumentException ex) {
        throw new InvalidAttributeException("must be a valid URI");
      }
      if (!PROTOCOL_PATTERN_COMPILED.matcher(str).find()) {
        throw new InvalidAttributeException(
            "URL protocol missing or not supported (must be one of %s)"
                .formatted(String.join(", ", SUPPORTED_PROTOCOLS)));
      }
    } else throw new InvalidAttributeException("must be a string");
  }

  @Override
  public Class<?> getValueType() {
    return String.class;
  }
}
