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
import java.util.Collection;
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
import org.veo.core.entity.definitions.ElementTypeDefinition;
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
                    ValidationError.localized(
                        "error_invalid_catalog_item_domain", ciDomain.getName()));
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
    var etd = ca.getDomain().getElementTypeDefinition(element.getType());
    var caDefinition = etd.getCustomAspectDefinition(ca.getType());
    // TODO #919 include translated CA name in the error message
    var errors =
        new ArrayList<>(
            AttributeValidator.getErrors(
                ca.getAttributes(), caDefinition.getAttributeDefinitions(), etd.getTranslations()));
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
          ValidationError.customAspectConflict(
              ca.getType(), ca.getDomain(), conflictingDomains, element));
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
    ElementTypeDefinition etd = domain.getElementTypeDefinition(modelType);
    var linkDefinition = etd.getLinks().get(linkType);
    if (linkDefinition == null) {
      return List.of(
          ValidationError.localized(
              "error_link_type_not_defined", linkType, modelType.getSingularTerm()));
    }
    var errors = new ArrayList<ValidationError>();
    errors.addAll(getLinkTargetTypeErrors(linkType, target.getType(), linkDefinition));
    errors.addAll(getLinkTargetSubTypeErrors(linkType, target, domain, linkDefinition));
    errors.addAll(
        ValidationError.mergeIfAny(
            ValidationError.localized(
                "error_invalid_link_attributes", List.of(l -> etd.findTranslation(l, linkType))),
            AttributeValidator.getErrors(
                attributes, linkDefinition.getAttributeDefinitions(), etd.getTranslations())));
    return errors;
  }

  private static List<ValidationError> getLinkTargetTypeErrors(
      String linkType, ElementType targetType, LinkDefinition linkDefinition) {
    if (linkDefinition.getTargetType() != targetType) {
      return List.of(
          ValidationError.localized(
              "error_invalid_link_target_type", targetType.getSingularTerm(), linkType));
    }
    return new ArrayList<>();
  }

  private static List<ValidationError> getLinkTargetSubTypeErrors(
      String linkType, Element target, Domain domain, LinkDefinition linkDefinition) {
    var targetSubType = target.findSubType(domain).orElse(null);
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      return List.of(
          ValidationError.localized(
              "error_invalid_link_target_sub_type",
              linkType,
              target.getName(),
              linkDefinition.getTargetSubType(),
              targetSubType));
    }
    return new ArrayList<>();
  }

  private static List<ValidationError> getControlImplementationCustomAspectErrors(
      RiskAffected<?, ?> riskAffected, Domain domain) {
    var ciDef =
        domain
            .getElementTypeDefinition(riskAffected.getType())
            .getControlImplementationDefinition();
    return riskAffected.getControlImplementations().stream()
        .map(
            ci -> {
              var ciCAs = ci.getCustomAspects(domain);
              if (ciCAs.isEmpty()) {
                return new ArrayList<ValidationError>();
              }
              if (!ci.getControl().getDomains().contains(domain)) {
                return List.of(
                    ValidationError.localized(
                        "error_ci_custom_aspect_domain_not_associated_with_control",
                        domain.getName()));
              }
              return ciCAs.entrySet().stream()
                  .map(
                      entry -> {
                        String caType = entry.getKey();
                        Map<String, Object> attributes = entry.getValue();
                        if (ciDef == null || !ciDef.getCustomAspects().containsKey(caType)) {
                          return List.of(
                              ValidationError.localized(
                                  "error_ci_custom_aspect_not_defined",
                                  caType,
                                  riskAffected.getType().getSingularTerm()));
                        }
                        var caDefinition = ciDef.getCustomAspects().get(caType);
                        return AttributeValidator.getErrors(
                            attributes,
                            caDefinition.getAttributeDefinitions(),
                            ciDef.getTranslations());
                      })
                  .flatMap(Collection::stream)
                  .toList();
            })
        .flatMap(Collection::stream)
        .toList();
  }
}
