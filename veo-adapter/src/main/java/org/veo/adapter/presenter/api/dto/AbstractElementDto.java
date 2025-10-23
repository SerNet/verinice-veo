/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.adapter.presenter.api.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.openapi.IdRefOwner;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.CustomLinkState;
import org.veo.core.entity.state.DomainAssociationState;
import org.veo.core.entity.state.ElementState;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/** Base transfer object for Elements. Contains common data for all Element DTOs. */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(schema = FullAssetDto.class, value = Asset.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullControlDto.class, value = Control.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullDocumentDto.class, value = Document.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullIncidentDto.class, value = Incident.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullPersonDto.class, value = Person.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullProcessDto.class, value = Process.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullScenarioDto.class, value = Scenario.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullScopeDto.class, value = Scope.SINGULAR_TERM),
    },
    oneOf = {
      FullAssetDto.class,
      FullControlDto.class,
      FullDocumentDto.class,
      FullIncidentDto.class,
      FullPersonDto.class,
      FullProcessDto.class,
      FullScenarioDto.class,
      FullScopeDto.class
    })
public abstract class AbstractElementDto<T extends Element>
    extends AbstractVersionedSelfReferencingDto implements NameableDto, ElementState<T> {

  @ToString.Include private UUID id;

  @Schema(
      description = "The name for the Element.",
      example = "Lock doors",
      requiredMode = REQUIRED)
  @ToString.Include
  private String name;

  @Schema(
      description = "Compact human-readable identifier that is unique within the client.",
      example = "A-155",
      accessMode = Schema.AccessMode.READ_ONLY)
  @ToString.Include
  private String designator;

  @Schema(description = "The abbreviation for the Element.", example = "Lock doors")
  private String abbreviation;

  @Schema(description = "The description for the Element.", example = "Lock doors")
  private String description;

  @NotNull(message = "An owner must be present.")
  @Schema(requiredMode = REQUIRED, implementation = IdRefOwner.class)
  private IdRef<Unit> owner;

  // TODO #2542 remove from OpenApi docs, but not for legacy endpoints
  // TODO #2543 remove
  @Valid
  @Schema(description = "Custom relations which do not affect the behavior.", title = "CustomLink")
  private Map<String, List<@Valid CustomLinkDto>> links = Collections.emptyMap();

  // TODO #2542 remove from OpenApi docs, but not for legacy endpoints
  // TODO #2543 remove
  @Valid
  @Schema(
      description = "Groups of customizable attributes - see '/schemas'",
      title = "CustomAspect")
  private Map<String, CustomAspectDto> customAspects = Collections.emptyMap();

  @Schema(description = "Entity type identifier", accessMode = Schema.AccessMode.READ_ONLY)
  private String type;

  public abstract void clearDomains();

  public abstract void transferToDomain(UUID sourceDomainId, UUID targetDomainId);

  private Set<CustomAspectState> getCustomAspectStates(UUID domainId) {
    return customAspects.entrySet().stream()
        .filter(
            e ->
                e.getValue().getDomains().isEmpty()
                    || e.getValue().getDomains().stream().anyMatch(d -> d.getId().equals(domainId)))
        .map(
            e ->
                new CustomAspectState.CustomAspectStateImpl(
                    e.getKey(), e.getValue().getAttributes()))
        .collect(Collectors.toSet());
  }

  private Set<CustomLinkState> getCustomLinkStates(UUID domainId) {
    return links.entrySet().stream()
        .flatMap(
            e ->
                e.getValue().stream()
                    .filter(
                        l ->
                            l.getDomains().isEmpty()
                                || l.getDomains().stream()
                                    .anyMatch(d -> d.getId().equals(domainId)))
                    .map(
                        l ->
                            new CustomLinkState.CustomLinkStateImpl(
                                e.getKey(), l.getAttributes(), l.getTarget())))
        .collect(Collectors.toSet());
  }

  public abstract Map<UUID, ? extends DomainAssociationDto> getDomains();

  @JsonIgnore
  @Override
  public Set<DomainAssociationState> getDomainAssociationStates() {
    if (!customAspects.isEmpty() || !links.isEmpty()) {
      var domainIds = getDomains().keySet();
      if (domainIds.isEmpty()) {
        throw new IllegalArgumentException(
            "Element cannot contain custom aspects or links without being associated with a domain");
      }
    }
    return getDomains().entrySet().stream()
        .map(
            entry ->
                entry
                    .getValue()
                    .getDomainAssociationState(
                        TypedId.from(entry.getKey(), Domain.class),
                        getCustomAspectStates(entry.getKey()),
                        getCustomLinkStates(entry.getKey())))
        .collect(Collectors.toSet());
  }
}
