/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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
package org.veo.rest;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.EnumUtils;

import lombok.AllArgsConstructor;

/**
 * Spring Boot's default String->Enum converter considers case, so it won't convert "scope" to
 * org.veo.core.entity.EntityType.SCOPE for example. This converter converts the String to
 * upper-case before trying to find an enum value with the specified name.
 *
 * @see DomainController#updateDomainWithSchema(org.springframework.security.core.Authentication,
 *     String, org.veo.core.entity.EntityType, com.fasterxml.jackson.databind.JsonNode)
 */
@AllArgsConstructor
public class IgnoreCaseEnumConverter<E extends Enum<E>> extends PropertyEditorSupport {

  private Class<E> type;

  @Override
  public void setAsText(String text) {
    setValue(EnumUtils.getEnum(type, text.toUpperCase()));
  }
}
