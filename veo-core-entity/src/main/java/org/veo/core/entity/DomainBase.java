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

import static java.util.function.Function.identity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.domainmigration.DomainMigrationDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.state.DomainBaseState;
import org.veo.core.entity.state.ElementTypeDefinitionState;
import org.veo.core.entity.state.TemplateItemState;

public interface DomainBase
    extends Nameable,
        Identifiable,
        Versioned,
        DomainBaseState,
        TranslationProvider<NameAbbreviationAndDescription> {
  int AUTHORITY_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  int TEMPLATE_VERSION_MAX_LENGTH = 10;
  int DECISION_ID_MAX_LENGTH = 256;
  int INSPECTION_ID_MAX_LENGTH = 256;

  void setAuthority(@NotNull String aAuthority);

  void setTemplateVersion(@NotNull String aTemplateVersion);

  Set<CatalogItem> getCatalogItems();

  default Optional<CatalogItem> findCatalogItem(UUID symbolicId) {
    return getCatalogItems().stream()
        .filter(ci -> ci.getSymbolicId().equals(symbolicId))
        .findFirst();
  }

  @Override
  default Set<TemplateItemState<CatalogItem, DomainBase>> getCatalogItemStates() {
    return getCatalogItems().stream()
        .map(ci -> (TemplateItemState<CatalogItem, DomainBase>) ci)
        .collect(Collectors.toSet());
  }

  void setCatalogItems(Set<CatalogItem> catalogItems);

  Set<ElementTypeDefinition> getElementTypeDefinitions();

  @Override
  default Map<ElementType, ElementTypeDefinitionState> getElementTypeDefinitionStates() {
    return getElementTypeDefinitions().stream()
        .collect(Collectors.toMap(ElementTypeDefinition::getElementType, identity()));
  }

  void setElementTypeDefinitions(Set<ElementTypeDefinition> elementTypeDefinitions);

  void applyElementTypeDefinition(ElementTypeDefinition definition);

  void setIncarnationConfiguration(IncarnationConfiguration incarnationConfiguration);

  // TODO #3860 remove again, rely on DomainBaseState method
  ControlImplementationConfiguration getControlImplementationConfiguration();

  // TODO #3860 remove again
  @Override
  default ControlImplementationConfigurationDto getControlImplementationConfigurationDto() {
    return new ControlImplementationConfigurationDto(getControlImplementationConfiguration());
  }

  void setControlImplementationConfiguration(
      @NotNull ControlImplementationConfiguration controlImplementationConfiguration);

  default Optional<ElementTypeDefinition> findElementTypeDefinition(ElementType type) {
    return getElementTypeDefinitions().stream()
        .filter(d -> d.getElementType().equals(type))
        .findFirst();
  }

  default ElementTypeDefinition getElementTypeDefinition(ElementType type) {
    return findElementTypeDefinition(type)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Domain has no definition for entity type %s", type)));
  }

  @Override
  default Set<ProfileState> getProfileStates() {
    return getProfiles().stream().map(p -> (ProfileState) p).collect(Collectors.toSet());
  }

  Optional<RiskDefinition> findRiskDefinition(String riskDefinitionId);

  Set<Profile> getProfiles();

  void setProfiles(Set<Profile> profiles);

  void setRiskDefinitions(Map<String, RiskDefinition> definitions);

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

  /**
   * @return {@code true} if this domain contains a definition for given custom aspect type that is
   *     identical to given definition, otherwise {@code false}
   */
  default boolean containsCustomAspectDefinition(
      ElementType elementType, String caType, CustomAspectDefinition definition) {
    return findCustomAspectDefinition(elementType, caType).map(definition::equals).orElse(false);
  }

  default Optional<CustomAspectDefinition> findCustomAspectDefinition(
      ElementType elementType, String caType) {
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

  default void removeProfile(UUID profileId) {
    if (!getProfiles().removeIf(p -> p.getId().equals(profileId))) {
      throw new NotFoundException(profileId, Profile.class);
    }
  }

  @Override
  DomainMigrationDefinition getDomainMigrationDefinition();

  void setDomainMigrationDefinition(DomainMigrationDefinition domainUpdateDescription);

  void setTranslations(Translated<NameAbbreviationAndDescription> translations);
}
