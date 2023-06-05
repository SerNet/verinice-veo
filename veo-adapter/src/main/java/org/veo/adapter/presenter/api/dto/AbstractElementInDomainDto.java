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
package org.veo.adapter.presenter.api.dto;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static org.veo.core.entity.aspects.SubTypeAspect.STATUS_DESCRIPTION;
import static org.veo.core.entity.aspects.SubTypeAspect.STATUS_MAX_LENGTH;
import static org.veo.core.entity.aspects.SubTypeAspect.STATUS_NOT_NULL_MESSAGE;
import static org.veo.core.entity.aspects.SubTypeAspect.SUB_TYPE_DESCRIPTION;
import static org.veo.core.entity.aspects.SubTypeAspect.SUB_TYPE_MAX_LENGTH;
import static org.veo.core.entity.aspects.SubTypeAspect.SUB_TYPE_NOT_NULL_MESSAGE;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefOwner;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.CustomLinkState;
import org.veo.core.entity.state.DomainAssociationState;
import org.veo.core.entity.state.ElementState;
import org.veo.core.usecase.service.TypedId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * An element from the viewpoint of a domain. Contains both basic and domain-specific properties.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractElementInDomainDto<TElement extends Element>
    extends AbstractVersionedDto
    implements NameableDto, ElementState<TElement>, DomainAssociationState {
  @JsonIgnore
  @Getter(AccessLevel.NONE)
  private ElementInDomainIdRef<TElement> selfRef;

  @JsonProperty(value = "_self", access = READ_ONLY)
  @Schema(description = "Absolute URL for this element in this domain", format = "uri")
  public String getSelf() {
    return Optional.ofNullable(selfRef)
        .map(ElementInDomainIdRef::getTargetInDomainUri)
        .orElse(null);
  }

  @JsonIgnore private TypedId<Domain> domain;

  @Size(min = 1, max = NAME_MAX_LENGTH)
  private String name;

  @Size(min = 1, max = ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Size(min = 1, max = DESCRIPTION_MAX_LENGTH)
  private String description;

  @JsonProperty(access = READ_ONLY)
  private String designator;

  @Schema(description = "Unit that this element belongs to", implementation = IdRefOwner.class)
  @NotNull(message = "An owner must be present.")
  private IdRef<ElementOwner> owner;

  @Schema(description = SUB_TYPE_DESCRIPTION)
  @NotNull(message = SUB_TYPE_NOT_NULL_MESSAGE)
  @Size(min = 1, max = SUB_TYPE_MAX_LENGTH)
  private String subType;

  @Schema(description = STATUS_DESCRIPTION)
  @NotNull(message = STATUS_NOT_NULL_MESSAGE)
  @Size(min = 1, max = STATUS_MAX_LENGTH)
  private String status;

  @Schema(
      description =
          "Domain-specific relations to other elements that contain a target element and a set of domain-specific attributes - available link types are specified in the domain's element type definition. For each link type, multiple target elements can be linked. The map uses link types as keys and lists of links as values.")
  @Valid
  private LinkMapDto links = new LinkMapDto();

  @Schema(
      description =
          "Domain-specific sets of attributes - available custom aspect types are specified in the domain's element type definition. The map uses custom aspect types as keys and custom aspects as values.",
      example =
          "{\"address\": {\"street\":\"Bahnhofsallee 1b\", \"postalCode\": \"37081\", \"city\": \"Göttingen\"}}")
  @Valid
  private CustomAspectMapDto customAspects = new CustomAspectMapDto();

  @Schema(
      description =
          "Results of all decisions concerning this element within this domain. Key is decision key, value is results.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = READ_ONLY)
  private Map<DecisionRef, DecisionResult> decisionResults;

  @Schema(description = "Element type identifier", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = READ_ONLY)
  public String getType() {
    return EntityType.getSingularTermByType(getModelInterface());
  }

  @JsonIgnore
  @Override
  public Set<CustomAspectState> getCustomAspectStates() {
    return customAspects.getValue().entrySet().stream()
        .map(e -> new CustomAspectState.CustomAspectStateImpl(e.getKey(), e.getValue().getValue()))
        .collect(Collectors.toSet());
  }

  @JsonIgnore
  @Override
  public Set<CustomLinkState> getCustomLinkStates() {
    return links.getValue().entrySet().stream()
        .flatMap(
            e ->
                e.getValue().stream()
                    .map(
                        l ->
                            new CustomLinkState.CustomLinkStateImpl(
                                e.getKey(), l.getAttributes().getValue(), l.getTarget())))
        .collect(Collectors.toSet());
  }

  @JsonIgnore
  @Override
  public Set<DomainAssociationState> getDomainAssociationStates() {
    return Set.of(this);
  }
}
