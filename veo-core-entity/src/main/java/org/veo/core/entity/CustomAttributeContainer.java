/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.entity;

import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * A custom aspect - it describes a subset of an {@link Element} object's attributes in a set of
 * domains. Attributes must conform to the dynamic object schema. A custom aspect is for
 * documentation purposes only - it may be edited by humans and presented to humans (e.g. in a web
 * form or generated report), but must never be used for any other computations such as risk
 * calculations.
 */
public interface CustomAttributeContainer {

  int TYPE_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;

  @NotNull
  String getType();

  void setType(String aType);

  DomainBase getDomain();

  void setDomain(DomainBase domain);

  Map<String, Object> getAttributes();

  /**
   * @return {@code true} if any attributes have changed
   */
  boolean setAttributes(Map<String, Object> attributes);
}
