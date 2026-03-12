/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
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
import lombok.NonNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ControlImplementationInDomainDto implements ControlImplementationState {
  @Schema(implementation = ElementInDomainIdRef.class)
  @NotNull
  ElementInDomainIdRef<Control> control;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  ImplementationStatus implementationStatus;

  @Schema(description = "Explanation why this control should be implemented on this element")
  @Size(min = 1, max = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH)
  String description;

  @Schema(
      description =
          "Person who is responsible for whether this control should be implemented on this element",
      implementation = ElementInDomainIdRef.class)
  ElementInDomainIdRef<Person> responsible;

  @JsonIgnore RequirementImplementationsRef requirementImplementationsRef;

  @JsonProperty(value = "_requirementImplementations", access = JsonProperty.Access.READ_ONLY)
  String getRequirementImplementationsUri() {
    return requirementImplementationsRef.getUrl();
  }

  @Schema(
      description = "Owner of the control implementation",
      implementation = ElementInDomainIdRef.class)
  ElementInDomainIdRef<RiskAffected<?, ?>> owner;

  @Schema(description = "Custom aspects for this control implementation")
  @Builder.Default
  CustomAspectMapDto customAspects = new CustomAspectMapDto();

  public static ControlImplementationInDomainDto from(
      ControlImplementation entity, ReferenceAssembler referenceAssembler, @NonNull Domain domain) {
    return ControlImplementationInDomainDto.builder()
        .control(ref(entity.getControl(), referenceAssembler, domain))
        .description(entity.getDescription())
        .responsible(ref(entity.getResponsible(), referenceAssembler, domain))
        .requirementImplementationsRef(
            RequirementImplementationsRef.from(entity, referenceAssembler))
        .owner(ref(entity.getOwner(), referenceAssembler, domain))
        .implementationStatus(
            entity.getOwner().getRequirementImplementations().stream()
                .filter(ri -> ri.getControl().equals(entity.getControl()))
                .findAny()
                .get()
                .getStatus())
        .customAspects(CustomAspectMapDto.from(entity.getCustomAspects(domain)))
        .build();
  }

  private static <T extends Element> ElementInDomainIdRef<T> ref(
      T element, ReferenceAssembler referenceAssembler, @NonNull Domain domain) {
    return ElementInDomainIdRef.from(element, domain, referenceAssembler);
  }

  @Override
  public Set<CustomAspectState> getCustomAspectStates(UUID domainId) {
    return customAspects.getValue().entrySet().stream()
        .map(e -> new CustomAspectState.CustomAspectStateImpl(e.getKey(), e.getValue().getValue()))
        .collect(Collectors.toSet());
  }
}
