/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.adapter.presenter.api.response;

import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Person;
import org.veo.core.entity.Task;
import org.veo.core.entity.TaskType;
import org.veo.core.entity.compliance.RequirementImplementation;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "EXPERIMENTAL API, SUBJECT TO CHANGE! A task that has yet to be performed in a unit - tasks are transient, i.e. they are produced by the API on demand and cannot be created, updated or deleted by API clients.",
    accessMode = Schema.AccessMode.READ_ONLY,
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(
          value = RequirementImplementation.SINGULAR_TERM,
          schema = RequirementImplementationTaskDto.class),
      @DiscriminatorMapping(
          value = RequirementImplementation.SINGULAR_TERM + "-revision",
          schema = RequirementImplementationTaskDto.class),
      @DiscriminatorMapping(value = "inspection-finding", schema = InspectionFindingTaskDto.class),
    },
    oneOf = {
      RequirementImplementationTaskDto.class,
      InspectionFindingTaskDto.class,
    })
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = RequirementImplementationTaskDto.class),
  @JsonSubTypes.Type(value = InspectionFindingTaskDto.class)
})
public sealed interface TaskDto permits InspectionFindingTaskDto, RequirementImplementationTaskDto {
  @NotNull
  @JsonProperty("type")
  TaskType type();

  @Schema(implementation = ElementInDomainIdRef.class)
  @Nullable
  @JsonProperty("assignee")
  ElementInDomainIdRef<Person> assignee();

  @Schema(implementation = ElementInDomainIdRef.class)
  @NotNull
  @JsonProperty("element")
  ElementInDomainIdRef<?> element();

  static TaskDto from(Task task, Domain domain, ReferenceAssembler referenceAssembler) {
    ElementInDomainIdRef<Person> assignee =
        Optional.ofNullable(task.assignee())
            .map(p -> ElementInDomainIdRef.from(p, domain, referenceAssembler))
            .orElse(null);
    ElementInDomainIdRef<Element> element =
        Optional.ofNullable(task.element())
            .map(p -> ElementInDomainIdRef.from(p, domain, referenceAssembler))
            .orElse(null);
    return switch (task.type()) {
      case REQUIREMENT_IMPLEMENTATION, REQUIREMENT_IMPLEMENTATION_REVISION ->
          new RequirementImplementationTaskDto(
              task.type(),
              element,
              assignee,
              Optional.ofNullable(task.requirementImplementation())
                  .map(ri -> RequirementImplementationDto.from(ri, referenceAssembler))
                  .orElse(null),
              task.deadline());
      case INSPECTION_FINDING ->
          new InspectionFindingTaskDto(
              task.type(), element, assignee, task.severity(), task.summary());
    };
  }
}
