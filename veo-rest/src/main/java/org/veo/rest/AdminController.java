/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.io.mapper.UnitDumpMapper;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.client.DeleteClientUseCase;
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(AdminController.URL_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public class AdminController {
  private final UseCaseInteractor useCaseInteractor;
  private final DeleteClientUseCase deleteClientUseCase;
  private final GetUnitDumpUseCase getUnitDumpUseCase;
  private final UpdateAllClientDomainsUseCase updateAllClientDomainsUseCase;
  private final EntityToDtoTransformer entityToDtoTransformer;
  private final TaskExecutor threadPoolTaskExecutor;

  public static final String URL_BASE_PATH = "/admin";

  @DeleteMapping("/client/{clientId}")
  @Operation(
      summary =
          "Deletes client and all associated data (including units, domains, elements & risks)")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteClient(
      @PathVariable String clientId) {
    return useCaseInteractor.execute(
        deleteClientUseCase,
        new DeleteClientUseCase.InputData(Key.uuidFrom(clientId)),
        out -> ResponseEntity.noContent().build());
  }

  @GetMapping("/unit-dump/{unitId}")
  @Operation(summary = "Exports given unit, including unit metadata, domains, elements & risks")
  public CompletableFuture<UnitDumpDto> getUnitDump(@PathVariable String unitId) {
    return useCaseInteractor.execute(
        getUnitDumpUseCase,
        (Supplier<GetUnitDumpUseCase.InputData>) () -> UnitDumpMapper.mapInput(unitId),
        out -> UnitDumpMapper.mapOutput(out, entityToDtoTransformer));
  }

  @PostMapping("domain-templates/{id}/allclientsupdate")
  @Operation(
      summary = "Migrates all clients to the domain created from given domain template",
      description =
          "Runs as a background task. For each client, elements associated with a previous version of the domain are migrated to the given version and the old domain is deactivated.")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateAllClientDomains(
      @PathVariable String id) {
    log.info(
        "Submit updateAllClientDomainsUseCase task for domainTemplate: {}",
        id.replaceAll("[\r\n]", ""));
    threadPoolTaskExecutor.execute(
        new DelegatingSecurityContextRunnable(
            // TODO: VEO-1397 wrap this lambda to job/task, maybe submit the task as event
            () -> {
              log.info("Start of updateAllClientDomainsUseCase task");
              updateAllClientDomainsUseCase.executeAndTransformResult(
                  new UpdateAllClientDomainsUseCase.InputData(Key.uuidFrom(id)), out -> null);
              log.info("end of updateAllClientDomainsUseCase task");
            }));
    return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
  }
}
