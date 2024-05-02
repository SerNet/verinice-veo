/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.entity.condition;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;

import lombok.Data;

/**
 * Provides the root element that is being viewed by the expression context (e.g., the element that
 * is being inspected or on which an action is performed).
 */
@Data
public class CurrentElementExpression implements VeoExpression {
  @Override
  public Object getValue(Element element, Domain domain) {
    return element;
  }

  @Override
  public void selfValidate(DomainBase domain, String elementType) {
    // It's fine
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return EntityType.ELEMENT_TYPES.stream()
        .filter(et -> et.getSingularTerm().equals(elementType))
        .findAny()
        .get()
        .getType();
  }
}
