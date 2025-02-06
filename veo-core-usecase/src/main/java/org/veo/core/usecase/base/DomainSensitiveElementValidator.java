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
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.exception.CrossUnitReferenceException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.specification.ElementDomainsAreSubsetOfUnitDomains;
import org.veo.core.entity.specification.ElementOnlyReferencesAssociatedDomains;
import org.veo.core.entity.specification.ElementOnlyReferencesItsOwnUnitSpecification;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/** Validates elements considering domain-specific rules (e.g. element type definitions). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainSensitiveElementValidator {

  public static boolean isValid(Element element, Domain domain) {
    try {
      validate(element, domain);
      return true;
    } catch (IllegalArgumentException e) {
      log.warn(
          "element {} ({}) is invalid: {}",
          element.getName(),
          element.getIdAsString(),
          e.getMessage());
      return false;
    }
  }

  public static void validate(Element element) {
    if (!new ElementOnlyReferencesItsOwnUnitSpecification().test(element)) {
      throw new CrossUnitReferenceException();
    }
    if (!new ElementDomainsAreSubsetOfUnitDomains().test(element)) {
      throw new UnprocessableDataException(
          "Element can only be associated with its unit's domains");
    }
    if (!new ElementOnlyReferencesAssociatedDomains().test(element)) {
      throw new IllegalArgumentException(
          "Element cannot contain custom aspects or links for domains it is not associated with");
    }

    // TODO #3274: review this wrt. #3622
    if (element.getAppliedCatalogItems().size() > 1) {
      throw new IllegalArgumentException("Element has multiple catalog references");
    }
    element
        .getDomains()
        .forEach(
            domain -> {
              validate(element, domain);
            });
  }

  public static void validate(Element element, Domain domain) {
    element.getCustomAspects(domain).forEach(ca -> validateCustomAspect(element, ca));
    element
        .getLinks(domain)
        .forEach(
            link -> {
              validateLink(
                  link.getType(),
                  element,
                  link.getTarget(),
                  link.getAttributes(),
                  link.getDomain());
            });

    SubTypeValidator.validate(element, domain);

    element
        .findAppliedCatalogItem(domain)
        .ifPresent(
            catalogItem -> {
              DomainBase ciDomain = catalogItem.getDomainBase();
              if (!ciDomain.equals(domain)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Invalid catalog item reference from domain '%s'.", ciDomain.getName()));
              }
            });

    var riskRefProvider = DomainRiskReferenceProvider.referencesForDomain(domain);
    if (element instanceof RiskAffected<?, ?> riskAffected) {
      RiskValuesValidator.validateImpactValues(
          riskAffected.getImpactValues(domain), riskRefProvider);
    }
    if (element instanceof Scenario scenario) {
      RiskValuesValidator.validateScenarioRiskValues(
          scenario.getPotentialProbability(domain), riskRefProvider);
    }
  }

  static void validateLinkTargetType(
      String linkType, LinkDefinition linkDefinition, ElementType targetType) {
    if (!linkDefinition.getTargetType().equals(targetType)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid target type '%s' for link type '%s'",
              targetType.getSingularTerm(), linkType));
    }
  }

  private static void validateCustomAspect(Element element, CustomAspect ca) {
    var caDefinition =
        ca.getDomain()
            .getElementTypeDefinition(element.getType())
            .getCustomAspectDefinition(ca.getType());
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
    ElementType modelType = source.getType();
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
      String linkType, Domain domain, ElementType modelType) {
    var linkDefinition = domain.getElementTypeDefinition(modelType).getLinks().get(linkType);
    if (linkDefinition == null) {
      throw new IllegalArgumentException(
          String.format(
              "Link type '%s' is not defined for element type '%s'",
              linkType, modelType.getSingularTerm()));
    }
    return linkDefinition;
  }

  private static void validateLinkTargetType(
      String linkType, Element target, LinkDefinition linkDefinition) {
    var targetType = target.getType();
    validateLinkTargetType(linkType, linkDefinition, targetType);
  }

  private static void validateLinkTargetSubType(
      String linkType, Element target, Domain domain, LinkDefinition linkDefinition) {
    var targetSubType = target.findSubType(domain).orElse(null);
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      throw new IllegalArgumentException(
          String.format(
              "Expected target of link '%s' ('%s') to have sub type '%s' but found '%s'",
              linkType, target.getName(), linkDefinition.getTargetSubType(), targetSubType));
    }
  }
}
