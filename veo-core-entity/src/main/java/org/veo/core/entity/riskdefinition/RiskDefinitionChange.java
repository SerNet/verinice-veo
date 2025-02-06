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

import org.veo.core.entity.TranslationProvider;

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

  default boolean requiresRiskRecalculation() {
    return false;
  }

  default boolean requiresMigration() {
    return false;
  }

  default boolean requiresImpactInheritanceRecalculation() {
    return false;
  }

  public record NewRiskDefinition() implements RiskDefinitionChange {}

  public record ImpactLinks() implements RiskDefinitionChange {
    @Override
    public boolean requiresImpactInheritanceRecalculation() {
      return RiskDefinitionChange.super.requiresImpactInheritanceRecalculation();
    }
  }

  public record TranslationDiff() implements RiskDefinitionChange {}

  public record ColorDiff() implements RiskDefinitionChange {}

  public record RiskMatrixDiff(CategoryDefinition cat) implements RiskDefinitionChange {
    @Override
    public boolean requiresRiskRecalculation() {
      return true;
    }
  }

  public record RiskMatrixAdd(CategoryDefinition cat) implements RiskDefinitionChange {
    @Override
    public boolean requiresRiskRecalculation() {
      return true;
    }

    @Override
    public boolean requiresMigration() {
      return true;
    }
  }

  public record RiskMatrixRemove(CategoryDefinition cat) implements RiskDefinitionChange {
    @Override
    public boolean requiresMigration() {
      return true;
    }
  }

  public record RiskMatrixResize() implements RiskDefinitionChange {}

  public record ImplementationStateListResize() implements RiskDefinitionChange {}

  public record ImpactListResize(CategoryDefinition cat) implements RiskDefinitionChange {}

  public record ProbabilityListResize() implements RiskDefinitionChange {}

  public record CategoryListAdd(CategoryDefinition newCat) implements RiskDefinitionChange {
    @Override
    public boolean requiresRiskRecalculation() {
      return true;
    }

    @Override
    public boolean requiresMigration() {
      return true;
    }
  }

  public record CategoryListRemove(CategoryDefinition oldCat) implements RiskDefinitionChange {
    @Override
    public boolean requiresRiskRecalculation() {
      return true;
    }

    @Override
    public boolean requiresMigration() {
      return true;
    }
  }

  public record RiskValueListResize() implements RiskDefinitionChange {}

  static boolean requiresRiskRecalculation(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream().anyMatch(RiskDefinitionChange::requiresRiskRecalculation);
  }

  static boolean requiresMigration(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream().anyMatch(RiskDefinitionChange::requiresMigration);
  }

  static boolean requiresImpactInheritanceRecalculation(Set<RiskDefinitionChange> detectedChanges) {
    return detectedChanges.stream()
        .anyMatch(RiskDefinitionChange::requiresImpactInheritanceRecalculation);
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
            ProbabilityListResize::new,
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
