/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
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
package org.veo.core.entity.riskdefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.TranslationProvider;
import org.veo.core.entity.risk.CategoryRef;

public sealed interface RiskDefinitionChange
    permits RiskDefinitionChange.CategoryListAdd,
        RiskDefinitionChange.CategoryListRemove,
        RiskDefinitionChange.ColorDiff,
        RiskDefinitionChange.ImpactLinks,
        RiskDefinitionChange.ImpactListResize,
        RiskDefinitionChange.ImplementationStateListResize,
        RiskDefinitionChange.NewRiskDefinition,
        RiskDefinitionChange.ProbabilityListResize,
        RiskDefinitionChange.RiskMatrixAdd,
        RiskDefinitionChange.RiskMatrixDiff,
        RiskDefinitionChange.RiskMatrixRemove,
        RiskDefinitionChange.RiskMatrixResize,
        RiskDefinitionChange.RiskValueListResize,
        RiskDefinitionChange.TranslationDiff {

  @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
  @JsonGetter
  default String getChangeType() {
    return getClass().getSimpleName();
  }

  default List<CategoryRef> riskMatrixResizeCategories() {
    return Collections.emptyList();
  }

  default List<RiskDefinitionChangeEffect> getEffects() {
    return Collections.emptyList();
  }

  default List<CategoryRef> changedRiskMatrixCategories() {
    return Collections.emptyList();
  }

  public record NewRiskDefinition() implements RiskDefinitionChange {}

  public record ImpactLinks() implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(new RiskDefinitionChangeEffect.ImpactInheritanceRecalculation());
    }
  }

  public record TranslationDiff() implements RiskDefinitionChange {}

  public record ColorDiff() implements RiskDefinitionChange {}

  public record RiskMatrixDiff(@JsonIgnore CategoryDefinition cat) implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(new RiskDefinitionChangeEffect.RiskRecalculation());
    }

    @Override
    public List<CategoryRef> changedRiskMatrixCategories() {
      return categories();
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(cat));
    }
  }

  public record RiskMatrixAdd(@JsonIgnore CategoryDefinition cat) implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(
          new RiskDefinitionChangeEffect.RiskRecalculation(),
          new RiskDefinitionChangeEffect.RiskValueCategoryAddition(CategoryRef.from(cat)));
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(cat));
    }
  }

  public record RiskMatrixRemove(@JsonIgnore CategoryDefinition cat)
      implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(
          new RiskDefinitionChangeEffect.RiskValueCategoryRemoval(CategoryRef.from(cat)));
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(cat));
    }
  }

  public record RiskMatrixResize(@JsonIgnore CategoryDefinition cat)
      implements RiskDefinitionChange {

    @Override
    public List<CategoryRef> riskMatrixResizeCategories() {
      return categories();
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(cat));
    }
  }

  public record ImplementationStateListResize() implements RiskDefinitionChange {}

  public record ImpactListResize(@JsonIgnore CategoryDefinition cat)
      implements RiskDefinitionChange {

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(cat));
    }

    @JsonIgnore
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(
          new RiskDefinitionChangeEffect.RiskValueCategoryRemoval(CategoryRef.from(cat)),
          new RiskDefinitionChangeEffect.ImpactCategoryRemoval(CategoryRef.from(cat)));
    }
  }

  public record ProbabilityListResize(@JsonIgnore RiskDefinition oldRd)
      implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return oldRd.getCategories().stream()
          .map(CategoryRef::from)
          .map(RiskDefinitionChangeEffect.RiskValueCategoryRemoval::new)
          .map(RiskDefinitionChangeEffect.class::cast)
          .toList();
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return oldRd.getCategories().stream().map(CategoryRef::from).toList();
    }
  }

  public record CategoryListAdd(@JsonIgnore CategoryDefinition newCat)
      implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(
          new RiskDefinitionChangeEffect.RiskRecalculation(),
          new RiskDefinitionChangeEffect.RiskValueCategoryAddition(CategoryRef.from(newCat)));
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(newCat));
    }
  }

  public record CategoryListRemove(@JsonIgnore CategoryDefinition oldCat)
      implements RiskDefinitionChange {
    @Override
    public List<RiskDefinitionChangeEffect> getEffects() {
      return List.of(
          new RiskDefinitionChangeEffect.RiskRecalculation(),
          new RiskDefinitionChangeEffect.RiskValueCategoryRemoval(CategoryRef.from(oldCat)),
          new RiskDefinitionChangeEffect.ImpactCategoryRemoval(CategoryRef.from(oldCat)));
    }

    @JsonGetter
    public List<CategoryRef> categories() {
      return List.of(CategoryRef.from(oldCat));
    }
  }

  public record RiskValueListResize() implements RiskDefinitionChange {}

  static List<CategoryRef> riskMatrtixResizeCategories(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream()
        .map(RiskDefinitionChange::riskMatrixResizeCategories)
        .flatMap(Collection::stream)
        .distinct()
        .toList();
  }

  static List<CategoryRef> riskMatrtixChangedCategories(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream()
        .map(RiskDefinitionChange::changedRiskMatrixCategories)
        .flatMap(Collection::stream)
        .distinct()
        .toList();
  }

  static List<CategoryRef> removedImpactCategories(Set<RiskDefinitionChange> detectedChanges) {
    return getEffects(detectedChanges).stream()
        .filter(RiskDefinitionChangeEffect.ImpactCategoryRemoval.class::isInstance)
        .map(RiskDefinitionChangeEffect.ImpactCategoryRemoval.class::cast)
        .map(RiskDefinitionChangeEffect.ImpactCategoryRemoval::category)
        .distinct()
        .toList();
  }

  static List<CategoryRef> addedRiskValueCategories(Set<RiskDefinitionChange> detectedChanges) {
    return getEffects(detectedChanges).stream()
        .filter(RiskDefinitionChangeEffect.RiskValueCategoryAddition.class::isInstance)
        .map(RiskDefinitionChangeEffect.RiskValueCategoryAddition.class::cast)
        .map(RiskDefinitionChangeEffect.RiskValueCategoryAddition::category)
        .distinct()
        .toList();
  }

  static List<CategoryRef> removedRiskValueCategories(Set<RiskDefinitionChange> detectedChanges) {
    return getEffects(detectedChanges).stream()
        .filter(RiskDefinitionChangeEffect.RiskValueCategoryRemoval.class::isInstance)
        .map(RiskDefinitionChangeEffect.RiskValueCategoryRemoval.class::cast)
        .map(RiskDefinitionChangeEffect.RiskValueCategoryRemoval::category)
        .distinct()
        .toList();
  }

  static boolean isPropablilityChanged(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream()
        .anyMatch(RiskDefinitionChange.ProbabilityListResize.class::isInstance);
  }

  static boolean requiresRiskRecalculation(Set<RiskDefinitionChange> detectedChanges) {
    return containsEffect(detectedChanges, RiskDefinitionChangeEffect.RiskRecalculation.class);
  }

  private static boolean containsEffect(
      Set<RiskDefinitionChange> detectedChanges,
      Class<? extends RiskDefinitionChangeEffect> effectType) {
    return getEffects(detectedChanges).stream().anyMatch(effectType::isInstance);
  }

  static List<RiskDefinitionChangeEffect> getEffects(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream()
        .map(RiskDefinitionChange::getEffects)
        .flatMap(Collection::stream)
        .distinct()
        .toList();
  }

  static boolean requiresMigration(Set<RiskDefinitionChange> detectedChanges) {
    return isPropablilityChanged(detectedChanges)
        || !removedImpactCategories(detectedChanges).isEmpty()
        || !removedRiskValueCategories(detectedChanges).isEmpty()
        || !addedRiskValueCategories(detectedChanges).isEmpty();
  }

  static boolean requiresImpactInheritanceRecalculation(Set<RiskDefinitionChange> detectedChanges) {
    return containsEffect(
        detectedChanges, RiskDefinitionChangeEffect.ImpactInheritanceRecalculation.class);
  }

  static Set<RiskDefinitionChange> detectChanges(
      RiskDefinition oldRiskDef, RiskDefinition newRiskDef) {
    Set<RiskDefinitionChange> changes = new HashSet<>();
    if (!newRiskDef
        .getImpactInheritingLinks()
        .entrySet()
        .equals(oldRiskDef.getImpactInheritingLinks().entrySet())) {
      changes.add(new ImpactLinks());
    }
    changes.addAll(
        detectTranslationChanges(
            oldRiskDef.getImplementationStateDefinition(),
            newRiskDef.getImplementationStateDefinition()));
    changes.addAll(
        detectChanges(
            ImplementationStateListResize::new,
            oldRiskDef.getImplementationStateDefinition().getLevels(),
            newRiskDef.getImplementationStateDefinition().getLevels()));
    changes.addAll(
        detectTranslationChanges(oldRiskDef.getProbability(), newRiskDef.getProbability()));
    changes.addAll(
        detectChanges(
            () -> new ProbabilityListResize(oldRiskDef),
            oldRiskDef.getProbability().getLevels(),
            newRiskDef.getProbability().getLevels()));
    changes.addAll(
        detectChanges(
            RiskValueListResize::new, oldRiskDef.getRiskValues(), newRiskDef.getRiskValues()));

    Set<String> newCategories =
        new HashSet<>(newRiskDef.getCategories().stream().map(CategoryDefinition::getId).toList());
    newCategories.removeAll(
        oldRiskDef.getCategories().stream().map(DimensionDefinition::getId).toList());
    changes.addAll(
        newCategories.stream()
            .map(newRiskDef::getCategory)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(CategoryListAdd::new)
            .collect(Collectors.toSet()));
    oldRiskDef.getCategories().stream()
        .forEach(
            oldCat -> {
              newRiskDef.getCategories().stream()
                  .filter(c -> c.getId().equals(oldCat.getId()))
                  .findAny()
                  .ifPresentOrElse(
                      c -> {
                        changes.addAll(
                            detectMatrixChanges(oldCat.getValueMatrix(), c.getValueMatrix(), c));
                        changes.addAll(detectTranslationChanges(oldCat, c));
                        changes.addAll(
                            detectChanges(
                                () -> new ImpactListResize(c),
                                oldCat.getPotentialImpacts(),
                                c.getPotentialImpacts()));
                      },
                      () -> changes.add(new CategoryListRemove(oldCat)));
            });
    return changes;
  }

  private static Set<RiskDefinitionChange> detectTranslationChanges(
      TranslationProvider oldTP, TranslationProvider newTP) {
    if (oldTP.getTranslations().equals(newTP.getTranslations())) {
      return Collections.emptySet();
    }
    return Set.of(new TranslationDiff());
  }

  private static Set<RiskDefinitionChange> detectMatrixChanges(
      List<List<RiskValue>> oldValueMatrix,
      List<List<RiskValue>> newValueMatrix,
      CategoryDefinition dimension) {
      if (newValueMatrix == null && oldValueMatrix == null) return Collections.emptySet();
      if (newValueMatrix == null
          || oldValueMatrix == null
          || newValueMatrix.size() != oldValueMatrix.size()) {
        if (newValueMatrix != null) return Set.of(new RiskMatrixAdd(dimension));
        else return Set.of(new RiskMatrixRemove(dimension));
      }
      return IntStream.range(0, newValueMatrix.size())
          .mapToObj(
              index ->
                  detectChanges(
                      () -> new RiskMatrixDiff(dimension),
                      oldValueMatrix.get(index),
                      newValueMatrix.get(index)))
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
  }

  private static Set<RiskDefinitionChange> detectChanges(
      Supplier<RiskDefinitionChange> structureChangeSupplier,
      List<? extends DiscreteValue> oldLevels,
      List<? extends DiscreteValue> newLevels) {
    if (oldLevels.size() != newLevels.size()) {
      return Set.of(structureChangeSupplier.get());
    }
    return IntStream.range(0, oldLevels.size())
        .mapToObj(
            index ->
                detectChanges(structureChangeSupplier, oldLevels.get(index), newLevels.get(index)))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private static Set<RiskDefinitionChange> detectChanges(
      Supplier<RiskDefinitionChange> structureChangeSupplier,
      DiscreteValue oldLevel,
      DiscreteValue newLevel) {
    if (newLevel.getOrdinalValue() != oldLevel.getOrdinalValue()) {
      return Set.of(structureChangeSupplier.get());
    }

    var translationChanges = new HashSet<>(detectTranslationChanges(oldLevel, newLevel));
    if (!Objects.equals(oldLevel.getHtmlColor(), newLevel.getHtmlColor())) {
      translationChanges.add(new ColorDiff());
    }
    return translationChanges;
  }
}
