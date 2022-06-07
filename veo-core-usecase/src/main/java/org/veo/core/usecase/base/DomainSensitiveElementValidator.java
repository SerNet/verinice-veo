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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.definitions.LinkDefinition;

/** Validates elements considering domain-specific rules (e.g. element type definitions). */
class DomainSensitiveElementValidator {

  public static void validate(Element element) {
    element
        .getCustomAspects()
        .forEach(
            ca -> {
              // TODO VEO-661 use the domain that the custom aspect is associated with (as soon as
              // each custom aspect belongs to exactly one domain). Right now a custom aspect could
              // also belong to zero or multiple domains, both of which are problematic.
              var domain =
                  getDomains(element).stream()
                      .filter(
                          d ->
                              d.getElementTypeDefinition(element.getModelType())
                                  .getCustomAspects()
                                  .containsKey(ca.getType()))
                      .findAny()
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  String.format(
                                      "Custom aspect type '%s' is not defined in any domain used by the element.",
                                      ca.getType())));
              validateCustomAspect(element, ca, domain);
            });
    element
        .getLinks()
        .forEach(
            link -> {
              // TODO VEO-661 use the domain that the link object is associated with (as soon
              // as each link belongs to exactly one domain). Right now a link could also
              // belong to zero or multiple domains, both of which are problematic.
              var domain =
                  getDomains(element).stream()
                      .filter(
                          d ->
                              d.getElementTypeDefinition(element.getModelType())
                                  .getLinks()
                                  .containsKey(link.getType()))
                      .findAny()
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  String.format(
                                      "Link type '%s' is not defined in any domain used by the element.",
                                      link.getType())));
              validateLink(link.getType(), element, link.getTarget(), link.getAttributes(), domain);
            });
    element.getDomainTemplates().forEach(d -> SubTypeValidator.validate(element, d));
  }

  private static void validateCustomAspect(
      Element element, CustomAspect ca, DomainTemplate domain) {
    var caDefinition =
        domain
            .getElementTypeDefinition(element.getModelType())
            .getCustomAspects()
            .get(ca.getType());
    try {
      AttributeValidator.validate(ca.getAttributes(), caDefinition.getAttributeSchemas());
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
      DomainTemplate domain) {
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
      AttributeValidator.validate(attributes, linkDefinition.getAttributeSchemas());
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
      String linkType, Element target, DomainTemplate domain, LinkDefinition linkDefinition) {
    if (linkDefinition.getTargetSubType() == null) {
      return;
    }
    var targetSubType = target.getSubType(domain).orElse(null);
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid target sub type '%s' for link type '%s'", targetSubType, linkType));
    }
  }

  // TODO VEO-661 get rid of this mess
  private static List<DomainTemplate> getDomains(Element element) {
    var domains = new ArrayList<>(element.getDomainTemplates());
    element.getOwningClient().ifPresent(client -> domains.addAll(client.getDomains()));
    return domains;
  }
}
