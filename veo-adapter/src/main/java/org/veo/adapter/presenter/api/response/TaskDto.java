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

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Task;
import org.veo.core.entity.TaskType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(
    description =
        "EXPERIMENTAL API, SUBJECT TO CHANGE! A task that has yet to be performed in a unit - tasks are transient, i.e. they are produced by the API on demand and cannot be created, updated or deleted by API clients.",
    accessMode = Schema.AccessMode.READ_ONLY)
public class TaskDto {
  @NotNull TaskType type;
  @Nullable LocalDate deadline;
  @Nullable RequirementImplementationDto requirementImplementation;

  @Schema(implementation = ElementInDomainIdRef.class)
  @Nullable
  ElementInDomainIdRef<Person> assignee;

  public static TaskDto from(Task task, Domain domain, ReferenceAssembler referenceAssembler) {
    return new TaskDto(
        task.type(),
        task.deadline(),
        Optional.ofNullable(task.requirementImplementation())
            .map(ri -> RequirementImplementationDto.from(ri, referenceAssembler))
            .orElse(null),
        Optional.ofNullable(task.assignee())
            .map(p -> ElementInDomainIdRef.from(p, domain, referenceAssembler))
            .orElse(null));
  }
}
