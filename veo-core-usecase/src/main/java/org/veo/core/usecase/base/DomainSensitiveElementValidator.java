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

import java.util.Map;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.specification.ElementCustomAspectsHaveDomain;

/** Validates elements considering domain-specific rules (e.g. element type definitions). */
class DomainSensitiveElementValidator {

  public static void validate(Element element) {
    if (!new ElementCustomAspectsHaveDomain().test(element)) {
      throw new IllegalArgumentException(
          "Element cannot contain custom aspects or links without being associated with a domain");
    }
    element.getCustomAspects().forEach(ca -> validateCustomAspect(element, ca));
    element
        .getLinks()
        .forEach(
            link -> {
              validateLink(
                  link.getType(),
                  element,
                  link.getTarget(),
                  link.getAttributes(),
                  link.getDomain());
            });
    element.getDomainTemplates().forEach(d -> SubTypeValidator.validate(element, d));
  }

  private static void validateCustomAspect(Element element, CustomAspect ca) {
    var caDefinition =
        ca.getDomain()
            .getElementTypeDefinition(element.getModelType())
            .getCustomAspects()
            .get(ca.getType());
    try {
      AttributeValidator.validate(ca.getAttributes(), caDefinition.getAttributeDefinitions());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid attributes for custom aspect type '%s': %s", ca.getType(), ex.getMessage()),
          ex);
    }
  }

  static void validateLink(
      String linkType,
      Element source,
      Element target,
      Map<String, Object> attributes,
      DomainBase domain) {
    var linkDefinition =
        domain.getElementTypeDefinition(source.getModelType()).getLinks().get(linkType);
    if (linkDefinition == null) {
      throw new IllegalArgumentException(
          String.format(
              "Link type '%s' is not defined for element type '%s'",
              linkType, source.getModelType()));
    }
    validateLinkTargetType(linkType, target, linkDefinition);
    validateLinkTargetSubType(linkType, target, domain, linkDefinition);
    try {
      AttributeValidator.validate(attributes, linkDefinition.getAttributeDefinitions());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          String.format("Invalid attributes for link type '%s': %s", linkType, ex.getMessage()),
          ex);
    }
  }

  private static void validateLinkTargetType(
      String linkType, Element target, LinkDefinition linkDefinition) {
    var targetType = target.getModelType();
    if (!linkDefinition.getTargetType().equals(targetType)) {
      throw new IllegalArgumentException(
          String.format("Invalid target type '%s' for link type '%s'", targetType, linkType));
    }
  }

  private static void validateLinkTargetSubType(
      String linkType, Element target, DomainBase domain, LinkDefinition linkDefinition) {
    if (linkDefinition.getTargetSubType() == null) {
      return;
    }
    var targetSubType = target.findSubType(domain).orElse(null);
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      throw new IllegalArgumentException(
          String.format(
              "Expected target of link '%s' ('%s') to have sub type '%s' but found '%s'",
              linkType, target.getName(), linkDefinition.getTargetSubType(), targetSubType));
    }
  }
}
