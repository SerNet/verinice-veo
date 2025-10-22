/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jonas Jordan
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

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;

import java.util.Locale;
import java.util.Optional;

import org.veo.core.entity.NameAbbreviationAndDescription;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.risk.CategoryRef;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public sealed interface RiskDefinitionChangeEffect {
  TranslatedText getDescription();

  @Data
  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  sealed class RiskDefinitionChangeEffectImpl implements RiskDefinitionChangeEffect {
    private final TranslatedText description;
    private final CategoryRef category;
  }

  public record RiskRecalculation() implements RiskDefinitionChangeEffect {
    @Override
    public TranslatedText getDescription() {
      return TranslatedText.builder()
          .translation(ENGLISH, "Risk values are recalculated.")
          .translation(GERMAN, "Risikowerte werden neu berechnet.")
          .build();
    }
  }

  public record ImpactInheritanceRecalculation() implements RiskDefinitionChangeEffect {
    @Override
    public TranslatedText getDescription() {
      return TranslatedText.builder()
          .translation(
              ENGLISH,
              "Inherited impact values are recalculated for all assets, processes and scopes.")
          .translation(
              GERMAN,
              "Vererbte Auswirkungswerte werden neu berechnet für alle Assets, Prozesse und Scopes.")
          .build();
    }
  }

  final class RiskValueCategoryAddition extends RiskDefinitionChangeEffectImpl {

    public RiskValueCategoryAddition(CategoryDefinition categoryDefinition) {
      super(
          TranslatedText.builder()
              .translation(
                  ENGLISH,
                  "Risk values for the criterion '%s' are added to risks."
                      .formatted(getNameOrId(categoryDefinition, ENGLISH)))
              .translation(
                  GERMAN,
                  "Werte für das Kriterium '%s' werden Risiken hinzugefügt."
                      .formatted(getNameOrId(categoryDefinition, GERMAN)))
              .build(),
          CategoryRef.from(categoryDefinition));
    }
  }

  final class RiskValueCategoryRemoval extends RiskDefinitionChangeEffectImpl {

    public RiskValueCategoryRemoval(CategoryDefinition categoryDefinition) {
      super(
          TranslatedText.builder()
              .translation(
                  ENGLISH,
                  "Risk values for the criterion '%s' are removed from all risks."
                      .formatted(getNameOrId(categoryDefinition, ENGLISH)))
              .translation(
                  GERMAN,
                  "Werte für das Kriterium '%s' werden aus allen Risiken entfernt."
                      .formatted(getNameOrId(categoryDefinition, GERMAN)))
              .build(),
          CategoryRef.from(categoryDefinition));
    }
  }

  final class ImpactCategoryRemoval extends RiskDefinitionChangeEffectImpl {

    public ImpactCategoryRemoval(CategoryDefinition categoryDefinition) {
      super(
          TranslatedText.builder()
              .translation(
                  Locale.ENGLISH,
                  "Impact values for the criterion '%s' are removed from all assets, processes and scopes."
                      .formatted(getNameOrId(categoryDefinition, ENGLISH)))
              .translation(
                  Locale.GERMAN,
                  "Auswirkungswerte für das Kriterium '%s' werden von allen Assets, Prozessen und Scopes entfernt."
                      .formatted(getNameOrId(categoryDefinition, GERMAN)))
              .build(),
          CategoryRef.from(categoryDefinition));
    }
  }

  private static String getNameOrId(CategoryDefinition cd, Locale locale) {
    return Optional.ofNullable(cd.getTranslations(locale))
        .map(NameAbbreviationAndDescription::getName)
        .orElse(cd.getId());
  }
}
