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
package org.veo.core.entity;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.Rule;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.inspection.Severity;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

public interface DomainBase extends Nameable, Identifiable, Versioned {
  int AUTHORITY_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  int TEMPLATE_VERSION_MAX_LENGTH = 10;

  /** The authority of this domaintemplate. */
  @NotNull
  String getAuthority();

  void setAuthority(@NotNull String aAuthority);

  /** The version */
  String getTemplateVersion();

  void setTemplateVersion(@NotNull String aTemplateVersion);

  /** The catalog describing the template element of this domaintemplate. */
  Set<Catalog> getCatalogs();

  default void setCatalogs(Set<Catalog> catalogs) {
    getCatalogs().clear();
    catalogs.forEach(catalog -> catalog.setDomainTemplate(this));
    getCatalogs().addAll(catalogs);
  }

  boolean addToCatalogs(Catalog aCatalog);

  void removeFromCatalog(Catalog aCatalog);

  Set<ElementTypeDefinition> getElementTypeDefinitions();

  void setElementTypeDefinitions(Set<ElementTypeDefinition> elementTypeDefinitions);

  void applyElementTypeDefinition(ElementTypeDefinition definition);

  default Optional<ElementTypeDefinition> findElementTypeDefinition(String type) {
    return getElementTypeDefinitions().stream()
        .filter(d -> d.getElementType().equals(type))
        .findFirst();
  }

  default ElementTypeDefinition getElementTypeDefinition(String type) {
    return findElementTypeDefinition(type)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Domain has no definition for entity type %s", type)));
  }

  /** Returns a map of risk definitions grouped by their ID. */
  Map<String, RiskDefinition> getRiskDefinitions();

  Optional<RiskDefinition> getRiskDefinition(String riskDefinitionId);

  default Optional<RiskDefinition> getRiskDefinition(Key<String> riskDefinitionId) {
    return getRiskDefinition(riskDefinitionId.value());
  }

  Map<String, ProfileDefinition> getProfiles();

  default Optional<ProfileDefinition> findProfile(ProfileRef ref) {
    return Optional.ofNullable(getProfiles().get(ref.getKeyRef()));
  }

  void setProfiles(Map<String, ProfileDefinition> profiles);

  void setRiskDefinitions(Map<String, RiskDefinition> definitions);

  default Map<String, Decision> getDecisions() {
    // TODO VEO-1294 use configurable persisted decisions
    final var piaCa = "process_privacyImpactAssessment";
    return Map.of(
        "piaMandatory",
        new Decision(
            TranslatedText.builder()
                .translation(ENGLISH, "Data Protection Impact Assessment mandatory")
                .translation(GERMAN, "Datenschutz-Folgenabschätzung verpflichtend")
                .build(),
            Process.SINGULAR_TERM,
            "PRO_DataProcessing",
            List.of(
                new Rule(
                        null,
                        TranslatedText.builder()
                            .translation(ENGLISH, "Missing risk analysis")
                            .translation(GERMAN, "Fehlende Risikoanalyse")
                            .build())
                    .ifNoRiskValuesPresent(),
                new Rule(
                        false,
                        TranslatedText.builder()
                            .translation(
                                ENGLISH,
                                "Processing on list of the kinds of processing operations not subject to a Data Protection Impact Assessment")
                            .translation(GERMAN, "VT auf Negativliste")
                            .build())
                    .ifAttributeEquals(piaCa + "_listed_negative", piaCa + "_listed", piaCa),
                new Rule(
                        false,
                        TranslatedText.builder()
                            .translation(ENGLISH, "Part of a joint processing")
                            .translation(GERMAN, "Gemeinsame VT")
                            .build())
                    .ifAttributeEquals(true, piaCa + "_processingOperationAccordingArt35", piaCa),
                new Rule(
                        false,
                        TranslatedText.builder()
                            .translation(ENGLISH, "Other exclusions")
                            .translation(GERMAN, "Anderer Ausschlusstatbestand")
                            .build())
                    .ifAttributeEquals(true, piaCa + "_otherExclusions", piaCa),
                new Rule(
                        true,
                        TranslatedText.builder()
                            .translation(ENGLISH, "High risk present")
                            .translation(GERMAN, "Hohes Risiko vorhanden")
                            .build())
                    .ifMaxRiskGreaterThan(BigDecimal.valueOf(1)),
                new Rule(
                        true,
                        TranslatedText.builder()
                            .translation(
                                ENGLISH,
                                "Processing on list of the kinds of processing operations subject"
                                    + " to a Data Protection Impact Assessment")
                            .translation(GERMAN, "VT auf Positivliste")
                            .build())
                    .ifAttributeEquals(piaCa + "_listed_positive", piaCa + "_listed", piaCa),
                new Rule(
                        true,
                        TranslatedText.builder()
                            .translation(ENGLISH, "Two or more criteria applicable")
                            .translation(GERMAN, "Mehrere Kriterien zutreffend")
                            .build())
                    .ifAttributeSizeGreaterThan(1, piaCa + "_processingCriteria", piaCa),
                new Rule(
                        null,
                        TranslatedText.builder()
                            .translation(ENGLISH, "DPIA-relevant attributes incomplete")
                            .translation(GERMAN, "DSFA-relevante Attribute unvollständig")
                            .build())
                    .ifAttributeIsNull(piaCa + "_processingCriteria", piaCa)
                    .ifAttributeIsNull(piaCa + "_listed", piaCa)
                    .ifAttributeIsNull(piaCa + "_otherExclusions", piaCa)
                    .ifAttributeIsNull(piaCa + "_processingOperationAccordingArt35", piaCa)),
            false));
  }

  default Optional<Decision> getDecision(String decisionKey) {
    return Optional.ofNullable(getDecisions().get(decisionKey));
  }

  default Map<String, Inspection> getInspections() {
    // TODO VEO-1355 use configurable persisted inspections
    if (!getName().equals("DS-GVO")) {
      return new HashMap<>();
    }
    return Map.of(
        "dpiaMissing",
        new Inspection(
                Severity.WARNING,
                TranslatedText.builder()
                    .translation(
                        GERMAN,
                        "Datenschutz-Folgenabschätzung wurde nicht durchgeführt, sie ist aber "
                            + "erforderlich.")
                    .translation(
                        ENGLISH,
                        "Data Protection Impact Assessment was not carried out, but it is mandatory.")
                    .build(),
                Process.SINGULAR_TERM,
                "PRO_DataProcessing")
            .ifDecisionResultEquals(true, new DecisionRef("piaMandatory", this))
            .ifPartAbsent("PRO_DPIA")
            .suggestAddingPart("PRO_DPIA"));
  }

  /**
   * @return {@code true} if this domain contains a definition for given custom aspect type that is
   *     identical to given definition, otherwise {@code false}
   */
  default boolean containsCustomAspectDefinition(
      String elementType, String caType, CustomAspectDefinition definition) {
    return findCustomAspectDefinition(elementType, caType).map(definition::equals).orElse(false);
  }

  default Optional<CustomAspectDefinition> findCustomAspectDefinition(
      String elementType, String caType) {
    return Optional.ofNullable(
        getElementTypeDefinition(elementType).getCustomAspects().get(caType));
  }
}
