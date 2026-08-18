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
package org.veo.rest;

import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PATTERN;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.response.TaskDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.TaskQuery;
import org.veo.core.usecase.GetTasksUseCase;
import org.veo.core.usecase.UseCaseInteractor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(TaskController.URL_BASE_PATH)
public class TaskController {
  public static final String URL_BASE_PATH = "/" + Domain.PLURAL_TERM + "/{domainId}/tasks";

  private final ReferenceAssembler referenceAssembler;
  private final GetTasksUseCase getTasksUseCase;
  private final UseCaseInteractor useCaseInteractor;

  @Operation(
      summary = "EXPERIMENTAL API, SUBJECT TO CHANGE! Retrieve open tasks for a domain and unit")
  @GetMapping
  @ApiResponse(responseCode = "200", description = "Tasks loaded")
  @ApiResponse(
      responseCode = "404",
      description = "Unit or domain not found or unit not associated with domain")
  public CompletableFuture<ResponseEntity<PageDto<TaskDto>>> getTasks(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestParam(required = true, name = UNIT_PARAM) UUID unitId,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(value = SORT_COLUMN_PARAM, required = false, defaultValue = "DEADLINE")
          TaskQuery.SortCriterion sortCriterion,
      @RequestParam(value = SORT_ORDER_PARAM, required = false, defaultValue = "asc")
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder) {
    return useCaseInteractor
        .execute(
            getTasksUseCase,
            new GetTasksUseCase.InputData(
                TypedId.from(domainId, Domain.class),
                TypedId.from(unitId, Unit.class),
                PagingMapper.toConfig(pageSize, pageNumber, sortCriterion, sortOrder)),
            output ->
                PagingMapper.toPage(
                    output.page(), t -> TaskDto.from(t, output.domain(), referenceAssembler)))
        .thenApply(ResponseEntity::ok);
  }
}
