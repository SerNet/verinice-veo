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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.RequirementImplementationsRef;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.state.ControlImplementationState;
import org.veo.core.entity.state.CustomAspectState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ControlImplementationDto implements ControlImplementationState {
  @NotNull IdRef<Control> control;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  ImplementationStatus implementationStatus;

  @Schema(description = "Explanation why this control should be implemented on this element")
  @Size(min = 1, max = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH)
  String description;

  @Schema(
      description =
          "Person who is responsible for whether this control should be implemented on this element")
  IdRef<Person> responsible;

  @JsonIgnore RequirementImplementationsRef requirementImplementationsRef;

  @JsonProperty(value = "_requirementImplementations", access = JsonProperty.Access.READ_ONLY)
  String getRequirementImplementationsUri() {
    return requirementImplementationsRef.getUrl();
  }

  @Schema(description = "Owner of the control implementation")
  IdRef<RiskAffected<?, ?>> owner;

  @Schema(description = "Custom aspects for this control implementation")
  @Builder.Default
  Map<UUID, CustomAspectMapDto> customAspects = new HashMap<>();

  public static ControlImplementationDto from(
      ControlImplementation entity, ReferenceAssembler referenceAssembler, Set<Domain> domains) {
    return ControlImplementationDto.builder()
        .control(ref(entity.getControl(), referenceAssembler))
        .description(entity.getDescription())
        .responsible(ref(entity.getResponsible(), referenceAssembler))
        .requirementImplementationsRef(
            RequirementImplementationsRef.from(entity, referenceAssembler))
        .owner(ref(entity.getOwner(), referenceAssembler))
        .implementationStatus(
            entity.getOwner().getRequirementImplementations().stream()
                .filter(ri -> ri.getControl().equals(entity.getControl()))
                .findAny()
                .get()
                .getStatus())
        .customAspects(
            domains.stream()
                .collect(
                    Collectors.toMap(
                        Domain::getId, d -> CustomAspectMapDto.from(entity.getCustomAspects(d)))))
        .build();
  }

  private static <T extends Element> IdRef<T> ref(
      T element, ReferenceAssembler referenceAssembler) {
    return IdRef.from(element, referenceAssembler);
  }

  @Override
  public Set<CustomAspectState> getCustomAspectStates(UUID domainId) {
    return Optional.ofNullable(customAspects.get(domainId))
        .map(
            cas ->
                cas.getValue().entrySet().stream()
                    .map(
                        e ->
                            new CustomAspectState.CustomAspectStateImpl(
                                e.getKey(), e.getValue().getValue()))
                    .collect(Collectors.<CustomAspectState>toSet()))
        .orElseGet(Collections::emptySet);
  }
}
