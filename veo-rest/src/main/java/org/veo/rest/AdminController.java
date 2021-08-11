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

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.io.mapper.UnitDumpMapper;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.rest.security.AccountImpl;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AdminController.URL_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public class AdminController {
    private final UseCaseInteractor useCaseInteractor;
    private final GetUnitDumpUseCase getUnitDumpUseCase;
    private final EntityToDtoTransformer entityToDtoTransformer;
    private final ReferenceAssembler referenceAssembler;

    public static final String URL_BASE_PATH = "/admin";

    @GetMapping("/unit-dump/{unitId}")
    public CompletableFuture<UnitDumpDto> getUnitDump(@Parameter(hidden = true) Authentication auth,
            @PathVariable String unitId) {
        return useCaseInteractor.execute(getUnitDumpUseCase,
                                         (Supplier<GetUnitDumpUseCase.InputData>) () -> UnitDumpMapper.mapInput(AccountImpl.from(auth),
                                                                                                                unitId),
                                         out -> UnitDumpMapper.mapOutput(out,
                                                                         entityToDtoTransformer));
    }
}
