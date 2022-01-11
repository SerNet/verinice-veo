/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.domain.CreateDomainUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/**
 * REST service which provides methods to manage domain templates.
 * <p>
 * Uses async calls with {@code CompletableFuture} to parallelize long running
 * operations (i.e. network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *      "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(DomainTemplateController.URL_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public class DomainTemplateController {

    public static final String URL_BASE_PATH = "/" + DomainTemplate.PLURAL_TERM;

    private final UseCaseInteractor useCaseInteractor;
    private final CreateDomainUseCase createDomainUseCase;

    @PostMapping(value = "/{id}/createdomains")
    @Operation(summary = "Creates domains from a domain template")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Domain(s) created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomainFromTemplate(
            Authentication auth, @PathVariable String id,
            @RequestParam(value = "clientids", required = false) List<String> clientIds) {
        return useCaseInteractor.execute(createDomainUseCase,
                                         new CreateDomainUseCase.InputData(id,
                                                 Optional.ofNullable(clientIds)),
                                         out -> ResponseEntity.noContent()
                                                              .build());
    }
}