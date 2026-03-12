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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomAttributeContainer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.ValidationError;
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
    var errors = getErrors(element, domain);
    if (!errors.isEmpty()) {
      log.warn(
          "element {} ({}) is invalid: {}", element.getName(), element.getIdAsString(), errors);
      return false;
    }
    return true;
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
    ValidationError.throwOnErrors(getErrors(element, domain));
  }

  public static List<ValidationError> getErrors(Element element, Domain domain) {
    var errors = new ArrayList<ValidationError>();
    errors.addAll(
        element.getCustomAspects(domain).stream()
            .flatMap(ca -> getCustomAspectErrors(element, ca).stream())
            .toList());
    errors.addAll(
        element.getLinks(domain).stream()
            .flatMap(
                link ->
                    getLinkErrors(
                        link.getType(),
                        element,
                        link.getTarget(),
                        link.getAttributes(),
                        link.getDomain())
                        .stream())
            .toList());

    errors.addAll(
        SubTypeValidator.getErrors(
            domain,
            element.findSubType(domain).orElse(null),
            element.findStatus(domain).orElse(null),
            element.getType()));

    element
        .findAppliedCatalogItem(domain)
        .ifPresent(
            catalogItem -> {
              DomainBase ciDomain = catalogItem.getDomainBase();
              if (!ciDomain.equals(domain)) {
                errors.add(
                    new ValidationError.Generic(
                        String.format(
                            "Invalid catalog item reference from domain '%s'.",
                            ciDomain.getName())));
              }
            });

    var riskRefProvider = DomainRiskReferenceProvider.referencesForDomain(domain);
    if (element instanceof RiskAffected<?, ?> riskAffected) {
      errors.addAll(
          RiskValuesValidator.getImpactValueErrors(
              riskAffected.getImpactValues(domain), riskRefProvider));
      errors.addAll(getControlImplementationCustomAspectErrors(riskAffected, domain));
    }
    if (element instanceof Scenario scenario) {
      errors.addAll(
          RiskValuesValidator.getScenarioRiskValueErrors(
              scenario.getPotentialProbability(domain), riskRefProvider));
    }
    return errors;
  }

  static void validateLinkTargetType(
      String linkType, LinkDefinition linkDefinition, ElementType targetType) {
    ValidationError.throwOnErrors(getLinkTargetTypeErrors(linkType, targetType, linkDefinition));
  }

  private static List<ValidationError> getCustomAspectErrors(Element element, CustomAspect ca) {
    var caDefinition =
        ca.getDomain()
            .getElementTypeDefinition(element.getType())
            .getCustomAspectDefinition(ca.getType());
    var errors =
        ValidationError.mergeIfAny(
            String.format("Invalid attributes for custom aspect type '%s'", ca.getType()),
            AttributeValidator.getErrors(
                ca.getAttributes(), caDefinition.getAttributeDefinitions()));
    var conflictingDomains =
        element.getDomains().stream()
            .filter(d -> !d.equals(ca.getDomain()))
            .filter(
                d ->
                    d.containsCustomAspectDefinition(element.getType(), ca.getType(), caDefinition))
            .filter(
                otherDomain -> {
                  var attributesInOtherDomain =
                      element
                          .findCustomAspect(otherDomain, ca.getType())
                          .map(CustomAttributeContainer::getAttributes)
                          .orElse(Collections.emptyMap());
                  return !ca.getAttributes().equals(attributesInOtherDomain);
                })
            .toList();
    if (!conflictingDomains.isEmpty()) {
      errors.add(
          new ValidationError.CustomAspectConflict(ca.getType(), conflictingDomains, element));
    }
    return errors;
  }

  private static List<ValidationError> getLinkErrors(
      String linkType,
      Element source,
      Element target,
      Map<String, Object> attributes,
      Domain domain) {
    ElementType modelType = source.getType();
    var linkDefinition = domain.getElementTypeDefinition(modelType).getLinks().get(linkType);
    if (linkDefinition == null) {
      return List.of(
          new ValidationError.Generic(
              String.format(
                  "Link type '%s' is not defined for element type '%s'",
                  linkType, modelType.getSingularTerm())));
    }
    var errors = new ArrayList<ValidationError>();
    errors.addAll(getLinkTargetTypeErrors(linkType, target.getType(), linkDefinition));
    errors.addAll(getLinkTargetSubTypeErrors(linkType, target, domain, linkDefinition));
    errors.addAll(
        ValidationError.mergeIfAny(
            String.format("Invalid attributes for link type '%s'c", linkType),
            AttributeValidator.getErrors(attributes, linkDefinition.getAttributeDefinitions())));
    return errors;
  }

  private static List<ValidationError> getLinkTargetTypeErrors(
      String linkType, ElementType targetType, LinkDefinition linkDefinition) {
    if (linkDefinition.getTargetType() != targetType) {
      return List.of(
          new ValidationError.Generic(
              String.format(
                  "Invalid target type '%s' for link type '%s'",
                  targetType.getSingularTerm(), linkType)));
    }
    return new ArrayList<>();
  }

  private static List<ValidationError> getLinkTargetSubTypeErrors(
      String linkType, Element target, Domain domain, LinkDefinition linkDefinition) {
    var targetSubType = target.findSubType(domain).orElse(null);
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      return List.of(
          new ValidationError.Generic(
              String.format(
                  "Expected target of link '%s' ('%s') to have sub type '%s' but found '%s'",
                  linkType, target.getName(), linkDefinition.getTargetSubType(), targetSubType)));
    }
    return new ArrayList<>();
  }

  private static List<ValidationError> getControlImplementationCustomAspectErrors(
      RiskAffected<?, ?> riskAffected, Domain domain) {
    var ciDef =
        domain
            .getElementTypeDefinition(riskAffected.getType())
            .getControlImplementationDefinition();
    for (var ci : riskAffected.getControlImplementations()) {
      var ciCAs = ci.getCustomAspects(domain);
      if (ciCAs.isEmpty()) {
        continue;
      }
      if (!ci.getControl().getDomains().contains(domain)) {
        return List.of(
            new ValidationError.Generic(
                "Cannot add custom aspects to a control implementation for domain '%s' because the control is not associated with it."
                    .formatted(domain.getName())));
      }
      for (var entry : ciCAs.entrySet()) {
        String caType = entry.getKey();
        Map<String, Object> attributes = entry.getValue();
        if (ciDef == null || !ciDef.getCustomAspects().containsKey(caType)) {
          return List.of(
              new ValidationError.Generic(
                  "Custom aspect type '%s' is not defined for control implementations on element type '%s'."
                      .formatted(caType, riskAffected.getType().getSingularTerm())));
        }
        var caDefinition = ciDef.getCustomAspects().get(caType);
        try {
          AttributeValidator.validate(attributes, caDefinition.getAttributeDefinitions());
        } catch (IllegalArgumentException ex) {
          return List.of(
              new ValidationError.Generic(
                  "Invalid attributes for CI custom aspect type '%s': %s"
                      .formatted(caType, ex.getMessage())));
        }
      }
    }
    return List.of();
  }
}
