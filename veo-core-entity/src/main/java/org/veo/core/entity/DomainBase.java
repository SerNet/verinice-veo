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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

public interface DomainBase extends Nameable, Identifiable, Versioned {
  int AUTHORITY_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  int TEMPLATE_VERSION_MAX_LENGTH = 10;
  int DECISION_ID_MAX_LENGTH = 256;
  int INSPECTION_ID_MAX_LENGTH = 256;

  /** The authority of this domaintemplate. */
  @NotNull
  String getAuthority();

  void setAuthority(@NotNull String aAuthority);

  /** The version */
  String getTemplateVersion();

  void setTemplateVersion(@NotNull String aTemplateVersion);

  Set<CatalogItem> getCatalogItems();

  void setCatalogItems(Set<CatalogItem> catalogItems);

  Set<ElementTypeDefinition> getElementTypeDefinitions();

  void setElementTypeDefinitions(Set<ElementTypeDefinition> elementTypeDefinitions);

  void applyElementTypeDefinition(ElementTypeDefinition definition);

  /**
   * The default behavior when creating incarnation descriptions for catalog items from this domain
   */
  IncarnationConfiguration getIncarnationConfiguration();

  void setIncarnationConfiguration(IncarnationConfiguration incarnationConfiguration);

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

  @Deprecated
  Map<String, ProfileDefinition> getJsonProfiles();

  default Optional<ProfileDefinition> findProfile(ProfileRef ref) {
    return Optional.ofNullable(getJsonProfiles().get(ref.getKeyRef()));
  }

  @Deprecated
  void setProfiles(Map<String, ProfileDefinition> profiles);

  Set<Profile> getProfiles();

  void setProfiles(Set<Profile> profiles);

  void setRiskDefinitions(Map<String, RiskDefinition> definitions);

  Map<String, Decision> getDecisions();

  /**
   * @throws org.veo.core.entity.exception.UnprocessableDataException if any decisions are invalid
   */
  void setDecisions(Map<String, Decision> decisions);

  /**
   * Adds or updates decision with given key
   *
   * @return {@code true} if a new decision was added, {@code false} if an existing decision was
   *     updated
   * @throws org.veo.core.entity.exception.UnprocessableDataException if the decision is invalid
   */
  boolean applyDecision(String key, Decision decision);

  /**
   * @throws NotFoundException if decision does not exist
   */
  void removeDecision(String decisionKey);

  default Decision getDecision(String decisionKey) {
    return Optional.ofNullable(getDecisions().get(decisionKey))
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Decision '%s' not found in domain %s"
                        .formatted(decisionKey, getIdAsString())));
  }

  Map<String, Inspection> getInspections();

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

  default Inspection getInspection(String inspectionId) {
    return Optional.ofNullable(getInspections().get(inspectionId))
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Domain %s does not contain inspection '%s'", getIdAsString(), inspectionId));
  }

  void setInspections(Map<String, Inspection> inspections);

  boolean applyInspection(String inspectionId, Inspection inspection);

  void removeInspection(String inspectionId);
}
