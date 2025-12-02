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
package org.veo.persistence.entity.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

/** Custom mapping for use in JSONB DB columns. */
public class CustomObjectMapper extends ObjectMapper {
  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  public CustomObjectMapper() {
    registerModule(new BlackbirdModule());
    disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
  }
}
