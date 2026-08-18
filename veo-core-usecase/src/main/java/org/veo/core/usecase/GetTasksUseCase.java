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
package org.veo.core.usecase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Task;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.TaskQuery;
import org.veo.core.repository.TaskRepository;
import org.veo.core.repository.UnitRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetTasksUseCase
    implements TransactionalUseCase<GetTasksUseCase.InputData, GetTasksUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final TaskRepository taskRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var domain = domainRepository.getById(input.domainRef.getId(), userAccessRights.getClientId());
    var unit = unitRepository.getById(input.unitRef.getId(), userAccessRights);
    if (!unit.getDomains().contains(domain)) {
      throw new NotFoundException("Unit is not associated with domain.");
    }
    var query = taskRepository.queryTasks(domain, unit, userAccessRights);
    return new OutputData(query.execute(input.pagingConfiguration), domain);
  }

  @Valid
  public record InputData(
      @NotNull TypedId<Domain> domainRef,
      @NotNull TypedId<Unit> unitRef,
      @NotNull PagingConfiguration<TaskQuery.SortCriterion> pagingConfiguration)
      implements UseCase.InputData {}

  public record OutputData(@Valid PagedResult<Task, TaskQuery.SortCriterion> page, Domain domain)
      implements UseCase.OutputData {}
}
