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

import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.risk.CategoryRef;

public sealed interface RiskDefinitionChangeEffect {
  TranslatedText getDescription();

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

  public record RiskValueCategoryAddition(CategoryRef category)
      implements RiskDefinitionChangeEffect {
    @Override
    public TranslatedText getDescription() {
      return TranslatedText.builder()
          .translation(
              ENGLISH, "Risk values for category '%s' are added to risks.".formatted(category))
          .translation(
              GERMAN,
              "Werte für die Kategorie '%s' werden Risiken hinzugefügt.".formatted(category))
          .build();
    }
  }

  public record RiskValueCategoryRemoval(CategoryRef category)
      implements RiskDefinitionChangeEffect {
    @Override
    public TranslatedText getDescription() {
      return TranslatedText.builder()
          .translation(
              ENGLISH,
              "Risk values for category '%s' are removed from all risks.".formatted(category))
          .translation(
              GERMAN,
              "Werte für die Kategorie '%s' werden aus allen Risiken entfernt.".formatted(category))
          .build();
    }
  }

  public record ImpactCategoryRemoval(CategoryRef category) implements RiskDefinitionChangeEffect {
    @Override
    public TranslatedText getDescription() {
      return TranslatedText.builder()
          .translation(
              Locale.ENGLISH,
              "Impact values for category '%s' are removed from all assets, processes and scopes."
                  .formatted(category))
          .translation(
              Locale.GERMAN,
              "Auswirkungswerte für die Kategorie '%s' werden von allen Assets, Prozessen und Scopes entfernt."
                  .formatted(category))
          .build();
    }
  }
}
