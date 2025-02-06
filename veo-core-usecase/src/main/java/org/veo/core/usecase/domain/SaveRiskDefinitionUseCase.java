/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.domain;

import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.CATEGORY_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.COLOR_DIFF;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.IMPACT_LINKS;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.IMPACT_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.IMPLEMENTATION_STATE_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.NEW_RISK_DEFINITION;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.PROBABILITY_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.RISK_MATRIX_DIFF;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.RISK_MATRIX_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.RISK_VALUE_LIST_RESIZE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.validation.Valid;

import org.veo.core.entity.TranslationProvider;
import org.veo.core.entity.event.RiskDefinitionChangedEvent;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.DiscreteValue;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChangeType;
import org.veo.core.entity.riskdefinition.RiskValue;
import org.veo.core.repository.DomainRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveRiskDefinitionUseCase
    implements TransactionalUseCase<
        SaveRiskDefinitionUseCase.InputData, SaveRiskDefinitionUseCase.OutputData> {

  private final DomainRepository repository;
  private final EventPublisher eventPublisher;

  @Override
  public OutputData execute(InputData input) {
    var domain = repository.getById(input.domainId, input.authenticatedClientId);
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    Set<RiskDefinitionChangeType> detectedChanges = new HashSet<>();
    domain
        .getRiskDefinition(input.riskDefinitionRef)
        .ifPresentOrElse(
            rd -> detectedChanges.addAll(detectChanges(rd, input.riskDefinition)),
            () -> detectedChanges.add(NEW_RISK_DEFINITION));

    if (detectedChanges.stream().anyMatch(input.forbiddenChanges::contains)) {
      throw new UnprocessableDataException(
          "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: "
              + Arrays.stream(RiskDefinitionChangeType.values())
                  .filter(r -> !input.forbiddenChanges.contains(r))
                  .toList());
    }
    domain.applyRiskDefinition(input.riskDefinitionRef, input.riskDefinition);
    eventPublisher.publish(
        RiskDefinitionChangedEvent.from(domain, input.riskDefinition, detectedChanges, this));
    return new OutputData(detectedChanges.contains(NEW_RISK_DEFINITION));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  private Set<RiskDefinitionChangeType> detectChanges(
      RiskDefinition oldRiskDef, RiskDefinition newRiskDef) {
    Set<RiskDefinitionChangeType> changes = new HashSet<>();
    if (!newRiskDef
        .getImpactInheritingLinks()
        .entrySet()
        .equals(oldRiskDef.getImpactInheritingLinks().entrySet())) {
      changes.add(IMPACT_LINKS);
    }
    changes.addAll(
        detectTranslationChanges(
            oldRiskDef.getImplementationStateDefinition(),
            newRiskDef.getImplementationStateDefinition()));
    changes.addAll(
        detectChanges(
            IMPLEMENTATION_STATE_LIST_RESIZE,
            oldRiskDef.getImplementationStateDefinition().getLevels(),
            newRiskDef.getImplementationStateDefinition().getLevels()));
    changes.addAll(
        detectTranslationChanges(oldRiskDef.getProbability(), newRiskDef.getProbability()));
    changes.addAll(
        detectChanges(
            PROBABILITY_LIST_RESIZE,
            oldRiskDef.getProbability().getLevels(),
            newRiskDef.getProbability().getLevels()));
    changes.addAll(
        detectChanges(
            RISK_VALUE_LIST_RESIZE, oldRiskDef.getRiskValues(), newRiskDef.getRiskValues()));
    if (newRiskDef.getCategories().size() != oldRiskDef.getCategories().size()) {
      changes.add(CATEGORY_LIST_RESIZE);
    } else {
      IntStream.range(0, newRiskDef.getCategories().size())
          .forEach(
              index -> {
                CategoryDefinition oldCat = oldRiskDef.getCategories().get(index);
                CategoryDefinition newCat = newRiskDef.getCategories().get(index);
                changes.addAll(
                    detectMatrixChanges(oldCat.getValueMatrix(), newCat.getValueMatrix()));
                changes.addAll(detectTranslationChanges(oldCat, newCat));
                changes.addAll(
                    detectChanges(
                        IMPACT_LIST_RESIZE,
                        oldCat.getPotentialImpacts(),
                        newCat.getPotentialImpacts()));
              });
    }
    return changes;
  }

  private Set<RiskDefinitionChangeType> detectTranslationChanges(
      TranslationProvider oldTP, TranslationProvider newTP) {
    if (oldTP.getTranslations().equals(newTP.getTranslations())) {
      return Collections.emptySet();
    }
    return Set.of(RiskDefinitionChangeType.TRANSLATION_DIFF);
  }

  private Set<RiskDefinitionChangeType> detectMatrixChanges(
      List<List<RiskValue>> oldValueMatrix, List<List<RiskValue>> newValueMatrix) {
    if (newValueMatrix == null && oldValueMatrix == null) return Collections.emptySet();
    if (newValueMatrix == null
        || oldValueMatrix == null
        || newValueMatrix.size() != oldValueMatrix.size()) return Set.of(RISK_MATRIX_RESIZE);
    return IntStream.range(0, newValueMatrix.size())
        .mapToObj(
            index ->
                detectChanges(
                    RISK_MATRIX_DIFF, newValueMatrix.get(index), oldValueMatrix.get(index)))
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private Set<RiskDefinitionChangeType> detectChanges(
      RiskDefinitionChangeType structureChangeType,
      List<? extends DiscreteValue> oldLevels,
      List<? extends DiscreteValue> newLevels) {
    if (oldLevels.size() != newLevels.size()) {
      return Set.of(structureChangeType);
    }
    return IntStream.range(0, oldLevels.size())
        .mapToObj(
            index -> detectChanges(structureChangeType, oldLevels.get(index), newLevels.get(index)))
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private Set<RiskDefinitionChangeType> detectChanges(
      RiskDefinitionChangeType structureChangeType,
      DiscreteValue oldLevel,
      DiscreteValue newLevel) {
    if (newLevel.getOrdinalValue() != oldLevel.getOrdinalValue()) {
      return Set.of(structureChangeType);
    }

    var translationChanges = new HashSet<>(detectTranslationChanges(oldLevel, newLevel));
    if (!Objects.equals(oldLevel.getHtmlColor(), newLevel.getHtmlColor())) {
      translationChanges.add(COLOR_DIFF);
    }
    return translationChanges;
  }

  @Valid
  public record InputData(
      UUID authenticatedClientId,
      UUID domainId,
      String riskDefinitionRef,
      RiskDefinition riskDefinition,
      Set<RiskDefinitionChangeType> forbiddenChanges)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(boolean newRiskDefinition) implements UseCase.OutputData {}
}
