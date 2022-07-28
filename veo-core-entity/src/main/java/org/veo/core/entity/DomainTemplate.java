/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.Rule;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.inspection.Severity;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

/**
 * DomainTemplate The domaintemplare are managed by the system itself. The uuid is a named uui
 * generated as following: https://v.de/veo/domain-templates/DOMAIN-NAME/VERSION DOMAIN-NAME:
 * authority-name VERSION: version.revision
 */
public interface DomainTemplate extends Nameable, Identifiable, Versioned {
  String SINGULAR_TERM = "domaintemplate";
  String PLURAL_TERM = "domaintemplates";

  int AUTHORITY_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  int REVISION_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  int TEMPLATE_VERSION_MAX_LENGTH = 10;

  /** The authority of this domaintemplate. */
  String getAuthority();

  void setAuthority(String aAuthority);

  /** The version */
  String getTemplateVersion();

  void setTemplateVersion(String aTemplateVersion);

  /** The revision of the version. */
  String getRevision();

  void setRevision(String aRevision);

  /** The catalog describing the template element of this domaintemplate. */
  Set<Catalog> getCatalogs();

  default void setCatalogs(Set<Catalog> catalogs) {
    getCatalogs().clear();
    catalogs.forEach(catalog -> catalog.setDomainTemplate(this));
    getCatalogs().addAll(catalogs);
  }

  boolean addToCatalogs(Catalog aCatalog);

  void removeFromCatalog(Catalog aCatalog);

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return DomainTemplate.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  Set<ElementTypeDefinition> getElementTypeDefinitions();

  void setElementTypeDefinitions(Set<ElementTypeDefinition> elementTypeDefinitions);

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
            Map.of(
                "en", "Data Protection Impact Assessment mandatory",
                "de", "Datenschutz-Folgenabschätzung verpflichtend"),
            Process.SINGULAR_TERM,
            "PRO_DataProcessing",
            List.of(
                new Rule(
                        null,
                        Map.of(
                            "en",
                            "Risk analysis not carried out",
                            "de",
                            "Risikoanalyse VT nicht durchgeführt"))
                    .ifNoRiskValuesPresent(),
                new Rule(
                        false,
                        Map.of(
                            "en",
                                "Processing on list of the kinds of processing operations not subject to a Data Protection Impact Assessment",
                            "de", "VT auf Negativliste"))
                    .ifAttributeEquals(piaCa + "_listed_negative", piaCa + "_listed", piaCa),
                new Rule(
                        false,
                        Map.of(
                            "en", "Part of a joint processing",
                            "de", "Gemeinsame VT"))
                    .ifAttributeEquals(true, piaCa + "_processingOperationAccordingArt35", piaCa),
                new Rule(
                        false,
                        Map.of(
                            "en", "Other exclusions",
                            "de", "Anderer Ausschlusstatbestand"))
                    .ifAttributeEquals(true, piaCa + "_otherExclusions", piaCa),
                new Rule(
                        true,
                        Map.of(
                            "en", "High risk present",
                            "de", "Hohes Risiko vorhanden"))
                    .ifMaxRiskGreaterThan(BigDecimal.valueOf(1)),
                new Rule(
                        true,
                        Map.of(
                            "en",
                                "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                            "de", "VT auf Positivliste"))
                    .ifAttributeEquals(piaCa + "_listed_positive", piaCa + "_listed", piaCa),
                new Rule(
                        true,
                        Map.of(
                            "en", "Two or more criteria apply",
                            "de", "Mehrere Kriterien treffen zu"))
                    .ifAttributeSizeGreaterThan(1, piaCa + "_processingCriteria", piaCa))));
  }

  default Optional<Decision> getDecision(String decisionKey) {
    return Optional.ofNullable(getDecisions().get(decisionKey));
  }

  default Map<String, Inspection> getInspections() {
    // TODO VEO-1355 use configurable persisted inspections
    return Map.of(
        "dpiaMissing",
        new Inspection(
                Severity.WARNING,
                Map.of(
                    "de",
                    "Datenschutz-Folgenabschätzung wurde nicht durchgeführt, sie ist aber erforderlich.",
                    "en",
                    "Data Protection Impact Assessment was not carried out, but it is mandatory."),
                Process.SINGULAR_TERM,
                "PRO_DataProcessing")
            .ifDecisionResultEquals(true, new DecisionRef("piaMandatory", this))
            .ifPartAbsent("PRO_DPIA")
            .suggestAddingPart("PRO_DPIA"));
  }
}
