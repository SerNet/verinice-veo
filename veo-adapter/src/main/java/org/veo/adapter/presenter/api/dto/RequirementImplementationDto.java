/*
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
 */
package org.veo.adapter.presenter.api.dto;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.RequirementImplementationRef;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.compliance.Origination;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.state.RequirementImplementationState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.TooManyFields")
public class RequirementImplementationDto extends AbstractVersionedDto
    implements RequirementImplementationState {
  @JsonIgnore private RequirementImplementationRef selfRef;
  IdRef<RiskAffected<?, ?>> origin;
  IdRef<Control> control;
  IdRef<Person> responsible;
  ImplementationStatus status;

  @Min(0)
  Integer cost;

  @Schema(format = "date")
  String implementationDate;

  IdRef<Person> implementedBy;
  IdRef<Document> document;

  @Schema(format = "date")
  String lastRevisionDate;

  IdRef<Person> lastRevisionBy;

  @Schema(format = "date")
  String nextRevisionDate;

  IdRef<Person> nextRevisionBy;

  @Size(min = 1, max = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH)
  String implementationStatement;

  @NotNull(message = "Origination must be set.")
  Origination origination;

  @Schema(format = "date")
  String implementationUntil;

  public RequirementImplementationDto(
      RequirementImplementationRef selfRef,
      IdRef<RiskAffected<?, ?>> origin,
      IdRef<Control> control) {
    this.selfRef = selfRef;
    this.origin = origin;
    this.control = control;
  }

  @JsonProperty(value = "_self", access = JsonProperty.Access.READ_ONLY)
  public String getSelf() {
    return selfRef.getTargetUrl();
  }

  public static RequirementImplementationDto from(
      RequirementImplementation source,
      Domain domain,
      List<String> customAspectKeys,
      @NonNull ReferenceAssembler referenceAssembler) {
    return from(
        source,
        ElementInDomainIdRef.from(
            source.getControl(), domain, referenceAssembler, customAspectKeys),
        referenceAssembler);
  }

  public static RequirementImplementationDto from(
      RequirementImplementation source, @NonNull ReferenceAssembler referenceAssembler) {
    return from(source, IdRef.from(source.getControl(), referenceAssembler), referenceAssembler);
  }

  private static RequirementImplementationDto from(
      RequirementImplementation source,
      IdRef<Control> controlRef,
      ReferenceAssembler referenceAssembler) {
    var target =
        new RequirementImplementationDto(
            RequirementImplementationRef.from(source, referenceAssembler),
            IdRef.from(source.getOrigin(), referenceAssembler),
            controlRef);
    target.setStatus(source.getStatus());
    Optional.ofNullable(source.getResponsible())
        .map(r -> IdRef.from(r, referenceAssembler))
        .ifPresent(target::setResponsible);
    target.setImplementationStatement(source.getImplementationStatement());
    target.setOrigination(source.getOrigination());
    Optional.ofNullable(source.getImplementationUntil())
        .map(DateTimeFormatter.ISO_LOCAL_DATE::format)
        .ifPresent(target::setImplementationUntil);
    target.setCost(source.getCost());
    Optional.ofNullable(source.getImplementationDate())
        .map(DateTimeFormatter.ISO_LOCAL_DATE::format)
        .ifPresent(target::setImplementationDate);
    target.setImplementedBy(IdRef.from(source.getImplementedBy(), referenceAssembler));
    target.setDocument(IdRef.from(source.getDocument(), referenceAssembler));
    Optional.ofNullable(source.getLastRevisionDate())
        .map(DateTimeFormatter.ISO_LOCAL_DATE::format)
        .ifPresent(target::setLastRevisionDate);
    target.setLastRevisionBy(IdRef.from(source.getLastRevisionBy(), referenceAssembler));
    Optional.ofNullable(source.getNextRevisionDate())
        .map(DateTimeFormatter.ISO_LOCAL_DATE::format)
        .ifPresent(target::setNextRevisionDate);
    target.setNextRevisionBy(IdRef.from(source.getNextRevisionBy(), referenceAssembler));
    return target;
  }
}
