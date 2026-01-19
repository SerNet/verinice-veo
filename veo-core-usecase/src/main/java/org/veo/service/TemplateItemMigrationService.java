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
package org.veo.service;

import static org.veo.core.entity.ElementType.RISK_RELATED_ELEMENTS;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.isPropablilityChanged;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.removedImpactCategories;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.removedRiskValueCategories;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ProfileItemRepository;
import org.veo.core.usecase.base.TemplateItemValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrates domain-specific information on catalog items when an element type definition on a domain
 * is updated. Removes invalid custom aspects, link references, attributes and subtypes.
 */
@RequiredArgsConstructor
@Slf4j
public class TemplateItemMigrationService {

  private final ElementMigrationService elementMigrationService;
  private final CatalogItemRepository catalogItemRepository;
  private final ProfileItemRepository profileItemRepository;

  public void removeRiskDefinition(Domain domain, RiskDefinitionRef definitionRef) {
    log.info("remove riskdefinition: {}", definitionRef);
    var items = catalogItemRepository.findAllByDomain(domain);
    List<RiskDefinitionRef> validRiskDefinitionRefs =
        domain.getRiskDefinitions().values().stream().map(RiskDefinitionRef::from).toList();
    items.stream()
        .filter(ci -> RISK_RELATED_ELEMENTS.contains(ci.getElementType()))
        .forEach(
            templateItem -> removeRiskDefinitionFromItem(templateItem, validRiskDefinitionRefs));
    removeRiskDefinitionFromRiskTailorRef(items, validRiskDefinitionRefs);
    domain.getProfiles().forEach(p -> removeRiskDefinitionFromProfile(p, validRiskDefinitionRefs));
    log.info("migration done.");
  }

  private void removeRiskDefinitionFromProfile(Profile p, List<RiskDefinitionRef> keySet) {
    RISK_RELATED_ELEMENTS.stream()
        .forEach(
            type -> {
              Set<ProfileItem> items = profileItemRepository.findAllByProfile(p, type);
              items.stream()
                  .forEach(templateItem -> removeRiskDefinitionFromItem(templateItem, keySet));
              removeRiskDefinitionFromRiskTailorRef(items, keySet);
            });
  }

  private void removeRiskDefinitionFromItem(
      TemplateItem<?, ?> templateItem, List<RiskDefinitionRef> keySet) {
    TemplateItemAspects aspects = templateItem.getAspects();
    templateItem.setAspects(
        new TemplateItemAspects(
            removeInvalidKeys(aspects.impactValues(), keySet),
            removeInvalidKeys(aspects.scenarioRiskValues(), keySet),
            keySet.contains(aspects.scopeRiskDefinition()) ? aspects.scopeRiskDefinition() : null));
  }

  private void removeRiskDefinitionFromRiskTailorRef(
      Set<? extends TemplateItem<?, ?>> items, List<RiskDefinitionRef> keySet) {
    items.stream()
        .flatMap(ci -> ci.getTailoringReferences().stream())
        .filter(RiskTailoringReference.class::isInstance)
        .map(RiskTailoringReference.class::cast)
        .forEach(r -> r.setRiskDefinitions(removeInvalidKeys(r.getRiskDefinitions(), keySet)));
  }

  public void migrateRiskDefinitionChange(
      Domain domain, RiskDefinition rd, Set<RiskDefinitionChange> detectedChanges) {
    var items =
        Stream.concat(
                catalogItemRepository.findAllByDomain(domain).stream(),
                domain.getProfiles().stream().map(Profile::getItems).flatMap(Collection::stream))
            .filter(catalogItem -> RISK_RELATED_ELEMENTS.contains(catalogItem.getElementType()))
            .collect(Collectors.toSet());
    items.forEach(item -> migrateAspects(item, rd, detectedChanges));
    migrateAllRiskTailoringReference(items, rd, detectedChanges);
  }

  private void migrateAspects(
      TemplateItem<?, ?> item, RiskDefinition rd, Set<RiskDefinitionChange> detectedChanges) {
    TemplateItemAspects aspects = item.getAspects();

    item.setAspects(
        new TemplateItemAspects(
            aspects.impactValues() == null
                ? null
                : migrateImpactValues(rd, detectedChanges, aspects.impactValues()),
            aspects.scenarioRiskValues() == null
                ? null
                : migrateScenarioValues(rd, detectedChanges, aspects.scenarioRiskValues()),
            aspects.scopeRiskDefinition()));
  }

  private Map<RiskDefinitionRef, ImpactValues> migrateImpactValues(
      RiskDefinition rd,
      Set<RiskDefinitionChange> detectedChanges,
      Map<RiskDefinitionRef, ImpactValues> impactValues) {

    var impactCategoriesToUnset = removedImpactCategories(detectedChanges);
    ImpactValues valuesForRd = impactValues.get(rd.toRef());
    if (impactCategoriesToUnset.isEmpty() || valuesForRd == null) {
      return impactValues;
    }
    Map<RiskDefinitionRef, ImpactValues> result = new HashMap<>(impactValues);
    result.put(rd.toRef(), valuesForRd.withoutCategories(impactCategoriesToUnset));
    return result;
  }

  private Map<RiskDefinitionRef, PotentialProbability> migrateScenarioValues(
      RiskDefinition rd,
      Set<RiskDefinitionChange> detectedChanges,
      @NotNull Map<RiskDefinitionRef, PotentialProbability> scenarioRiskValues) {

    var scenarioValues = new HashMap<>(scenarioRiskValues);
    if (isPropablilityChanged(detectedChanges)) {
      scenarioValues.put(rd.toRef(), new PotentialProbability(null, null));
    }
    return scenarioValues;
  }

  private void migrateAllRiskTailoringReference(
      Set<? extends TemplateItem<?, ?>> items,
      RiskDefinition rd,
      Set<RiskDefinitionChange> detectedChanges) {
    items.stream()
        .flatMap(ci -> Set.copyOf(ci.getTailoringReferences()).stream())
        .filter(RiskTailoringReference.class::isInstance)
        .map(RiskTailoringReference.class::cast)
        .forEach(
            r -> {
              Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions =
                  r.getRiskDefinitions();
              Optional.ofNullable(riskDefinitions.get(rd.toRef()))
                  .ifPresent(
                      riskTailoringReferenceValues -> {
                        var categories = new HashMap<>(riskTailoringReferenceValues.categories());
                        removedRiskValueCategories(detectedChanges).forEach(categories::remove);
                        riskDefinitions.put(
                            rd.toRef(), riskTailoringReferenceValues.withCategories(categories));
                      });
            });
  }

  public void migrate(ElementType type, Domain domain) {
    migrateCatatlog(type, domain);
    domain.getProfiles().forEach(p -> migrateProfile(p, type));
  }

  private void migrateProfile(Profile p, ElementType type) {
    log.info(
        "migrate {} items in Profile {} ({})",
        type.getSingularTerm(),
        p.getName(),
        p.getIdAsString());
    Domain domain = p.requireDomainMembership();
    Set<ProfileItem> items = profileItemRepository.findAllByProfile(p, type);
    items.stream().forEach(templateItem -> migrate(templateItem, domain));
    migrateAllTailoringReferences(type, domain, items);
  }

  private void migrateCatatlog(ElementType type, Domain domain) {
    var items = catalogItemRepository.findAllByDomain(domain);

    // Migrate elements
    items.stream().filter(e -> e.getElementType() == type).forEach(e -> migrate(e, domain));
    migrateAllTailoringReferences(type, domain, items);
  }

  private void migrate(TemplateItem<?, ?> item, Domain domain) {
    var definition = domain.getElementTypeDefinition(item.getElementType());
    new HashMap<>(item.getCustomAspects())
        .entrySet()
        .forEach(
            entry -> {
              var caDefinition = definition.getCustomAspects().get(entry.getKey());
              if (caDefinition == null) {
                log.debug(
                    "Removing obsolete custom aspect {} from element {}.",
                    entry.getKey(),
                    item.getSymbolicIdAsString());
                item.getCustomAspects().remove(entry.getKey());
                return;
              }
              elementMigrationService.migrateAttributes(
                  entry.getValue(), caDefinition.getAttributeDefinitions());
            });
  }

  private void migrateAllTailoringReferences(
      ElementType type, Domain domain, Set<? extends TemplateItem<?, ?>> items) {
    items.stream()
        .flatMap(ci -> Set.copyOf(ci.getTailoringReferences()).stream())
        .filter(LinkTailoringReference.class::isInstance)
        .map(LinkTailoringReference.class::cast)
        .filter(ltr -> ltr.getLinkSourceItem().getElementType() == type)
        .forEach(
            linkTailoringReference ->
                domain
                    .getElementTypeDefinition(type)
                    .findLink(linkTailoringReference.getLinkType())
                    .ifPresentOrElse(
                        linkDef -> {
                          elementMigrationService.migrateAttributes(
                              linkTailoringReference.getAttributes(),
                              linkDef.getAttributeDefinitions());
                          try {
                            TemplateItemValidator.validate(linkTailoringReference, domain);
                          } catch (IllegalArgumentException illEx) {
                            log.info("Tailoring reference validation failed", illEx);
                            log.info(
                                "Deleting invalid tailoring reference {}",
                                linkTailoringReference.getIdAsString());
                            linkTailoringReference.remove();
                          }
                        },
                        () -> {
                          log.info(
                              "Link type {} for tailoring reference is invalid, deleting invalid tailoring reference {}",
                              linkTailoringReference.getLinkType(),
                              linkTailoringReference.getIdAsString());
                          linkTailoringReference.remove();
                        }));
  }

  private <TKey, TValue> Map<TKey, TValue> removeInvalidKeys(
      Map<TKey, TValue> map, List<TKey> validKeys) {
    if (map == null) return new HashMap<>();
    return map.entrySet().stream()
        .filter(e -> validKeys.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
