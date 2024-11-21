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

import static org.veo.core.entity.EntityType.RISK_RELETATED_ELEMENTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskRelated;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;
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
        .filter(RiskRelated.class::isInstance)
        .forEach(
            templateItem ->
                removeRiskDefinitionFromItem(domain, templateItem, validRiskDefinitionRefs));
    removeRiskDefinitionFromRiskTailorRef(domain, items, validRiskDefinitionRefs);

    domain
        .getProfiles()
        .forEach(p -> removeRiskDefinitionFromProfile(domain, p, validRiskDefinitionRefs));
    log.info("migration done.");
  }

  private void removeRiskDefinitionFromProfile(
      Domain domain, Profile p, List<RiskDefinitionRef> keySet) {
    RISK_RELETATED_ELEMENTS.stream()
        .forEach(
            type -> {
              Set<ProfileItem> items = profileItemRepository.findAllByProfile(p, type);
              items.stream()
                  .forEach(
                      templateItem -> removeRiskDefinitionFromItem(domain, templateItem, keySet));
              removeRiskDefinitionFromRiskTailorRef(domain, items, keySet);
            });
  }

  private void removeRiskDefinitionFromItem(
      Domain domain, TemplateItem<?, ?> templateItem, List<RiskDefinitionRef> keySet) {
    TemplateItemAspects aspects = templateItem.getAspects();
    templateItem.setAspects(
        new TemplateItemAspects(
            removeInvalidKeys(aspects.impactValues(), keySet),
            removeInvalidKeys(aspects.scenarioRiskValues(), keySet)));
  }

  private void removeRiskDefinitionFromRiskTailorRef(
      Domain domain, Set<? extends TemplateItem<?, ?>> items, List<RiskDefinitionRef> keySet) {
    items.stream()
        .flatMap(ci -> ci.getTailoringReferences().stream())
        .filter(RiskTailoringReference.class::isInstance)
        .map(RiskTailoringReference.class::cast)
        .forEach(r -> r.setRiskDefinitions(removeInvalidKeys(r.getRiskDefinitions(), keySet)));
  }

  public void migrateRiskDefinitionChange(Domain domain) {
    List<RiskDefinitionRef> validRiskDefinitionRefs =
        domain.getRiskDefinitions().values().stream().map(RiskDefinitionRef::from).toList();
    var items = catalogItemRepository.findAllByDomain(domain);

    // Migrate elements
    items.stream()
        .filter(RiskRelated.class::isInstance)
        .forEach(item -> migrateAspects(item, domain, validRiskDefinitionRefs));
    migrateAllRiskTailoringReference(domain, items);
    log.info("catalog migration done.");

    domain
        .getProfiles()
        .forEach(
            p -> {
              RISK_RELETATED_ELEMENTS.stream()
                  .forEach(
                      type -> {
                        Set<ProfileItem> pitems = profileItemRepository.findAllByProfile(p, type);
                        pitems.stream()
                            .forEach(
                                templateItem ->
                                    migrateAspects(templateItem, domain, validRiskDefinitionRefs));
                        migrateAllRiskTailoringReference(domain, pitems);
                      });
            });
    log.info("profiles migration done.");
  }

  private void migrateAspects(
      TemplateItem<?, ?> item, Domain domain, List<RiskDefinitionRef> validRiskDefinitionRefs) {
    TemplateItemAspects aspects = item.getAspects();
    item.setAspects(
        new TemplateItemAspects(
            Optional.ofNullable(aspects.impactValues())
                .map(
                    iv ->
                        syncMap(
                            aspects.impactValues(),
                            validRiskDefinitionRefs,
                            e ->
                                newImpactValues(
                                    e.getValue(), domain.getRiskDefinition(e.getKey().getIdRef()))))
                .orElse(null),
            Optional.ofNullable(aspects.scenarioRiskValues())
                .map(
                    srv ->
                        syncMap(
                            srv,
                            validRiskDefinitionRefs,
                            e ->
                                newPotentialProbability(
                                    e.getValue(), domain.getRiskDefinition(e.getKey().getIdRef()))))
                .orElse(null)));
  }

  private void migrateAllRiskTailoringReference(
      Domain domain, Set<? extends TemplateItem<?, ?>> items) {
    items.stream()
        .flatMap(ci -> Set.copyOf(ci.getTailoringReferences()).stream())
        .filter(RiskTailoringReference.class::isInstance)
        .map(RiskTailoringReference.class::cast)
        .forEach(
            r -> {
              Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions =
                  r.getRiskDefinitions();
              Map.copyOf(riskDefinitions)
                  .entrySet()
                  .forEach(
                      entry -> {
                        Optional<RiskDefinition> riskDefinition =
                            domain.getRiskDefinition(entry.getKey().getIdRef());
                        if (riskDefinition
                            .isPresent()) { // remove all categories not present in the domain
                          List<CategoryRef> allCategories =
                              new ArrayList<>(entry.getValue().categories().keySet());
                          RiskDefinition domainDefinition = riskDefinition.get();
                          List<CategoryRef> domainCategoryRefs =
                              domainDefinition.getCategories().stream()
                                  .map(c -> CategoryRef.from(c))
                                  .toList();
                          allCategories.removeAll(domainCategoryRefs);
                          RiskTailoringReferenceValues values =
                              newRiskTailoringReferenceValues(allCategories, entry.getValue());
                          riskDefinitions.put(entry.getKey(), values);
                          r.setRiskDefinitions(riskDefinitions);
                        } else {
                          // remove whole entry
                          riskDefinitions.remove(entry.getKey());
                          r.setRiskDefinitions(riskDefinitions);
                        }
                      });
            });
  }

  private <TKey, TValue> Map<TKey, TValue> syncMap(
      Map<TKey, TValue> map,
      List<TKey> validKeys,
      Function<Entry<TKey, TValue>, TValue> transformer) {
    return map.entrySet().stream()
        .filter(e -> validKeys.contains(e.getKey()))
        .collect(Collectors.toMap(e -> e.getKey(), e -> transformer.apply(e)));
  }

  private RiskTailoringReferenceValues newRiskTailoringReferenceValues(
      List<CategoryRef> keySet, RiskTailoringReferenceValues value) {
    var categories =
        value.categories().entrySet().stream()
            .filter(t -> !keySet.contains(t.getKey()))
            .collect(Collectors.toMap(t -> t.getKey(), t -> t.getValue()));

    return new RiskTailoringReferenceValues(
        value.specificProbability(), value.specificProbabilityExplanation(), categories);
  }

  private PotentialProbability newPotentialProbability(
      PotentialProbability value, Optional<RiskDefinition> riskDefinition) {
    if (riskDefinition.isPresent()) {
      RiskDefinition rd = riskDefinition.get();
      List<ProbabilityRef> levels =
          rd.getProbability().getLevels().stream().map(ProbabilityRef::from).toList();
      if (levels.contains(value.potentialProbability())) {
        return value;
      }
    }
    return null;
  }

  private ImpactValues newImpactValues(
      ImpactValues oldImpacts, Optional<RiskDefinition> riskDefinition) {
    if (riskDefinition.isPresent()) {
      RiskDefinition rd = riskDefinition.get();
      var validCats = rd.getCategories().stream().map(CategoryRef::from).toList();
      return new ImpactValues(
          removeInvalidKeys(oldImpacts.potentialImpacts(), validCats),
          removeInvalidKeys(oldImpacts.potentialImpactsCalculated(), validCats),
          removeInvalidKeys(oldImpacts.potentialImpactReasons(), validCats),
          removeInvalidKeys(oldImpacts.potentialImpactExplanations(), validCats));
    }
    return null;
  }

  public void migrate(EntityType type, Domain domain) {
    migrateCatatlog(type, domain);
    domain.getProfiles().forEach(p -> migrateProfile(p, type));
  }

  private void migrateProfile(Profile p, EntityType type) {
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

  private void migrateCatatlog(EntityType type, Domain domain) {
    var items = catalogItemRepository.findAllByDomain(domain);

    // Migrate elements
    items.stream()
        .filter(e -> e.getElementType().equals(type.getSingularTerm()))
        .forEach(e -> migrate(e, domain));

    // Migrate link tailoring references
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
      EntityType type, Domain domain, Set<? extends TemplateItem<?, ?>> items) {
    items.stream()
        .flatMap(ci -> Set.copyOf(ci.getTailoringReferences()).stream())
        .filter(LinkTailoringReference.class::isInstance)
        .map(LinkTailoringReference.class::cast)
        .filter(ltr -> ltr.getLinkSourceItem().getElementType().equals(type.getSingularTerm()))
        .forEach(
            linkTailoringReference ->
                domain
                    .getElementTypeDefinition(type.getSingularTerm())
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
