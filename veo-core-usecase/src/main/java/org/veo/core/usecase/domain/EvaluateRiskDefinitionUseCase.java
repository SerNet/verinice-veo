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
package org.veo.core.usecase.domain;

import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.detectChanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.inspection.Severity;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.ProbabilityDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.entity.riskdefinition.RiskDefinitionChangeEffect;
import org.veo.core.entity.riskdefinition.RiskValue;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EvaluateRiskDefinitionUseCase
    implements TransactionalUseCase<
        EvaluateRiskDefinitionUseCase.InputData, EvaluateRiskDefinitionUseCase.OutputData> {

  private final TranslatedText riskMatrixResizeText =
      TranslatedText.builder()
          .translation(
              Locale.ENGLISH,
              "The following risk matrices have been resized, please adjust the risk values if necessary: %s")
          .translation(
              Locale.GERMAN,
              "Die folgenden Risikomatrizen wurden in ihrer Größe angepasst, bitte passen Sie die Risikowerte gegebenenfalls an: %s")
          .build();
  private final TranslatedText riskMatrixChangeText =
      TranslatedText.builder()
          .translation(
              Locale.ENGLISH,
              "Risk matrices have been changed. Please adjust the risk values for the following criteria: %s")
          .translation(
              Locale.GERMAN,
              "Folgende risikomatrizen sind verändert worden, bitte passen Sie ggf. die Risiko Werte an: %s")
          .build();

  private TranslatedText riskMatrixValueInconsistencyText =
      TranslatedText.builder()
          .translation(
              Locale.ENGLISH, "The risk matrices for the following criteria are inconsistent: %s")
          .translation(
              Locale.GERMAN, "Die Risikomatrizen für die folgenden Kriterien sind inkonsistent: %s")
          .build();

  private final DomainRepository repository;

  @Override
  public OutputData execute(InputData input) {
    var domain = repository.getActiveById(input.domainId, input.authenticatedClientId);

    if (input.riskDefinition == null) {
      RiskDefinition riskDefinition =
          domain.getRiskDefinition(input.riskDefinitionRef).orElseThrow();
      return new OutputData(
          riskDefinition,
          Collections.emptyList(),
          validationMessages(riskDefinition, Set.of()),
          Collections.emptyList());
    }
    RiskValue last = input.riskDefinition.getRiskValues().getLast();

    input.riskDefinition.getCategories().stream()
        .filter(CategoryDefinition::isRiskValuesSupported)
        .forEach(cd -> syncRiskMatrix(input.riskDefinition.getProbability(), cd, last));

    Set<RiskDefinitionChange> detectedChanges = new HashSet<>();
    domain
        .getRiskDefinition(input.riskDefinitionRef)
        .ifPresentOrElse(
            rd -> detectedChanges.addAll(detectChanges(rd, input.riskDefinition)),
            () -> detectedChanges.add(new RiskDefinitionChange.NewRiskDefinition()));

    if (detectedChanges.stream().anyMatch(c -> !input.allowedChanges.contains(c.getClass()))) {
      throw new UnprocessableDataException(
          "Your modifications on this existing risk definition are not supported yet. Currently, only the following changes are allowed: "
              + input.allowedChanges.stream().map(Class::getSimpleName).sorted().toList());
    }

    return new OutputData(
        input.riskDefinition,
        detectedChanges.stream().toList(),
        validationMessages(input.riskDefinition, detectedChanges),
        RiskDefinitionChange.getEffects(detectedChanges));
  }

  private void addValidationMessage(
      Severity serverty,
      TranslatedText translated,
      List<CategoryRef> categories,
      List<ValidationMessage> effects) {
    if (!categories.isEmpty()) {
      effects.add(
          new ValidationMessage(
              serverty, toTRanslation(translated, categories), categories, null, null));
    }
  }

  private TranslatedText toTRanslation(TranslatedText translated, List<CategoryRef> categories) {
    Map<Locale, String> formatedText =
        translated.getTranslations().entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, v -> v.getValue().formatted(categories)));
    return new TranslatedText(formatedText);
  }

  private List<ValidationMessage> validationMessages(
      RiskDefinition riskDefinition, Set<RiskDefinitionChange> detectedChanges) {
    List<ValidationMessage> messages = new ArrayList<>();
    riskDefinition
        .getCategories()
        .forEach(
            cat -> {
              try {
                cat.validateRiskCategory(
                    riskDefinition.getRiskValues(), riskDefinition.getProbability());
              } catch (IllegalArgumentException e) {
                addValidationMessage(
                    Severity.ERROR,
                    // TODO translate error messages
                    TranslatedText.builder()
                        .translation(Locale.ENGLISH, e.getMessage())
                        .translation(Locale.GERMAN, e.getMessage())
                        .build(),
                    List.of(CategoryRef.from(cat)),
                    messages);
              }
            });

    riskDefinition.getCategories().stream()
        .filter(CategoryDefinition::isRiskValuesSupported)
        .filter(cat -> !cat.getValueMatrix().isEmpty())
        .forEach(
            cat -> {
              List<CategoryRef> categories = List.of(CategoryRef.from(cat));
              var vm = cat.getValueMatrix();
              int rows = vm.size();
              int columns = vm.getFirst().size();
              for (int row = 0; row < rows; row++) {
                for (int column = 1; column < columns; column++) {
                  RiskValue current = vm.get(row).get(column);
                  RiskValue previousColumnValue = vm.get(row).get(column - 1);
                  if (current.getOrdinalValue() < previousColumnValue.getOrdinalValue()) {
                    messages.add(
                        new ValidationMessage(
                            Severity.WARNING,
                            toTRanslation(riskMatrixValueInconsistencyText, categories),
                            categories,
                            row,
                            column));
                  }
                  if (row != 0) {
                    RiskValue previousRowValue = vm.get(row - 1).get(column);
                    if (current.getOrdinalValue() < previousRowValue.getOrdinalValue()) {
                      messages.add(
                          new ValidationMessage(
                              Severity.WARNING,
                              toTRanslation(riskMatrixValueInconsistencyText, categories),
                              categories,
                              row,
                              column));
                    }
                  }
                }
              }
            });
    addValidationMessage(
        Severity.WARNING,
        riskMatrixResizeText,
        RiskDefinitionChange.riskMatrtixResizeCategories(detectedChanges),
        messages);
    addValidationMessage(
        Severity.WARNING,
        riskMatrixChangeText,
        RiskDefinitionChange.riskMatrtixChangedCategories(detectedChanges),
        messages);
    return messages;
  }

  private void syncRiskMatrix(
      ProbabilityDefinition probability, CategoryDefinition category, RiskValue last) {
    if (!category.getPotentialImpacts().isEmpty()) {
      List<List<RiskValue>> valueMatrix = category.getValueMatrix();
      int columns = category.getPotentialImpacts().size();
      int rows = probability.getLevels().size();
      List<List<RiskValue>> rMatrix = new ArrayList<>(columns);
      for (int column = 0; column < columns; column++) {
        List<RiskValue> newRow = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
          newRow.add(riskValueOrDefault(valueMatrix, column, row, last));
        }
        rMatrix.add(newRow);
      }
      category.setValueMatrix(rMatrix);
    } else {
      category.setValueMatrix(null);
    }
  }

  private RiskValue riskValueOrDefault(
      List<List<RiskValue>> valueMatrix, int column, int row, RiskValue defaultValue) {
    if (column > valueMatrix.size() - 1) return defaultValue;
    if (row > valueMatrix.get(column).size() - 1) return defaultValue;
    RiskValue riskValue = valueMatrix.get(column).get(row);
    if (defaultValue.getOrdinalValue() < riskValue.getOrdinalValue()) return defaultValue;
    return riskValue;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  public record ValidationMessage(
      Severity severity,
      TranslatedText description,
      List<CategoryRef> changedCategories,
      @Min(0) Integer column,
      @Min(0) Integer row) {}

  @Valid
  public record InputData(
      UUID authenticatedClientId,
      UUID domainId,
      String riskDefinitionRef,
      RiskDefinition riskDefinition,
      Set<Class<? extends RiskDefinitionChange>> allowedChanges)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(
      RiskDefinition riskDefinition,
      List<RiskDefinitionChange> detectedChanges,
      List<ValidationMessage> message,
      List<RiskDefinitionChangeEffect> effects)
      implements UseCase.OutputData {}
}
