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
package org.veo.core.usecase.base;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

/** Validates an element's subtype / status according to the domains' element type definitions. */
class SubTypeValidator {
  static void validate(Element element, Domain domain) {
    element
        .findSubType(domain)
        .ifPresentOrElse(
            subType -> {
              validate(domain, subType, element.getStatus(domain), element.getModelType());
            },
            () -> {
              throw new IllegalArgumentException(
                  "Cannot assign element to domain without specifying a sub type");
            });
  }

  static void validate(DomainBase domain, String subType, String status, String modelType) {
    if (subType == null)
      throw new IllegalArgumentException(
          "Cannot assign element to domain without specifying a sub type");

    var definition = domain.getElementTypeDefinition(modelType).getSubTypes().get(subType);
    if (definition == null) {
      throw new IllegalArgumentException(
          String.format("Sub type '%s' is not defined for element type %s", subType, modelType));
    }
    if (!definition.getStatuses().contains(status)) {
      throw new IllegalArgumentException(
          String.format("Status '%s' is not allowed for sub type '%s'", status, subType));
    }
  }
}
