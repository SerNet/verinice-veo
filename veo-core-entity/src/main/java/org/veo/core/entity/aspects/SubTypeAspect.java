/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.entity.aspects;

import org.veo.core.entity.Constraints;

/**
 * Marks an entity as being of a sub type. Sub types are specific to the domain and the entity type.
 */
public interface SubTypeAspect extends Aspect {
  String SUB_TYPE_DESCRIPTION =
      "Domain-specific sub type - available sub types are listed in the domain's element type definition. The sub type cannot be changed once the element has been associated with the domain.";
  int SUB_TYPE_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  String SUB_TYPE_NOT_NULL_MESSAGE = "A sub type must be present";

  String STATUS_DESCRIPTION =
      "Domain-specific status - available statuses depend on the sub type and are specified in the domain's element type definition.";
  int STATUS_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  String STATUS_NOT_NULL_MESSAGE = "A status must be present";

  String getStatus();

  String getSubType();

  void setStatus(String status);
}
