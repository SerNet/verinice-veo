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
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.specification.ElementDomainsAreSubsetOfUnitDomains;
import org.veo.core.entity.specification.ElementIsAssociatedWithCustomAspectAndLinkDomains;

/** Validates elements considering domain-specific rules (e.g. element type definitions). */
public class DomainSensitiveElementValidator {

  public static void validate(Element element) {
    if (!new ElementDomainsAreSubsetOfUnitDomains().test(element)) {
      throw new UnprocessableDataException(
          "Element can only be associated with its unit's domains");
    }
    if (!new ElementIsAssociatedWithCustomAspectAndLinkDomains().test(element)) {
      throw new NotFoundException(
          "Element cannot contain custom aspects or links for domains it is not associated with");
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
    element.getDomains().forEach(d -> SubTypeValidator.validate(element, d));
  }

  static void validateLinkTargetType(
      String linkType, LinkDefinition linkDefinition, String targetType) {
    if (!linkDefinition.getTargetType().equals(targetType)) {
      throw new IllegalArgumentException(
          String.format("Invalid target type '%s' for link type '%s'", targetType, linkType));
    }
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

  private static void validateLink(
      String linkType,
      Element source,
      Element target,
      Map<String, Object> attributes,
      Domain domain) {
    String modelType = source.getModelType();
    var linkDefinition = getLinkDefinition(linkType, domain, modelType);
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

  private static LinkDefinition getLinkDefinition(
      String linkType, Domain domain, String modelType) {
    var linkDefinition = domain.getElementTypeDefinition(modelType).getLinks().get(linkType);
    if (linkDefinition == null) {
      throw new IllegalArgumentException(
          String.format(
              "Link type '%s' is not defined for element type '%s'", linkType, modelType));
    }
    return linkDefinition;
  }

  private static void validateLinkTargetType(
      String linkType, Element target, LinkDefinition linkDefinition) {
    var targetType = target.getModelType();
    validateLinkTargetType(linkType, linkDefinition, targetType);
  }

  private static void validateLinkTargetSubType(
      String linkType, Element target, Domain domain, LinkDefinition linkDefinition) {
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
