/*******************************************************************************
 * Copyright (c) 2018 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.veo.adapter.presenter.api.common.ApiResponse;
import org.veo.adapter.presenter.api.common.InvalidDateException;
import org.veo.adapter.presenter.api.dto.AssetItemDto;
import org.veo.adapter.presenter.api.dto.ProcessDto;
import org.veo.adapter.presenter.api.process.CreateProcessInputMapper;
import org.veo.adapter.presenter.api.process.CreateProcessOutputMapper;
import org.veo.adapter.usecase.interactor.UseCaseInteractor;
import org.veo.commons.VeoException;
import org.veo.core.entity.DomainException;
import org.veo.core.entity.Key;
import org.veo.core.usecase.asset.CreateAssetUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.model.HistoryEntry;
import org.veo.rest.security.ApplicationUser;
import org.veo.rest.security.CurrentUser;
import org.veo.service.ElementMapService;
import org.veo.service.HistoryService;

/**
 * Controller for the resource API of "Process" entities.
 *
 */
@RestController
@RequestMapping("/process")
public class ProcessController {
    
    private UseCaseInteractor useCaseInteractor;
    private CreateProcessUseCase createProcessUseCase;
    private GetProcessUseCase getProcessUseCase;
    
    public ProcessController(UseCaseInteractor useCaseInteractor, CreateProcessUseCase createProcessUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.createProcessUseCase = createProcessUseCase;
    }
    
    /**
     * Load the process for the given id. The result is provided asynchronously by the executed use case.
     * 
     * @param id an ID in the UUID format as specified in RFC 4122
     * @return the process for the given ID if one was found. Null otherwise.
     */
    @GetMapping("/{id}")
    CompletableFuture<ProcessDto> getProcessById(@PathVariable String id) {
        return useCaseInteractor.execute(
                getProcessUseCase,
                new GetProcessUseCase.InputData(Key.uuidFrom(id)),
                output -> (ProcessDto.from(output.getProcess()))
        );
    }


    /**
     * Create and persist a new process object for the given parameters.
     * 
     * @param user the currently logged in user. Provided by the authentication context.
     * @param dto the required fields to create a new process. Provided as request body.
     * @param requestTimezone the timezone in which the request originated. Should be set by the HTTP client. Server's timezone will be used if missing.
     * @return a resource URI for the newly created process.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // see: https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
    CompletableFuture<ResponseEntity<ApiResponse>> create(
            @CurrentUser CurrentUser user,
            @Valid @RequestBody ProcessDto dto,
            TimeZone requestTimezone 
            ) {
        try {
            
            final String timezoneId = requestTimezone.getID();
            return useCaseInteractor.execute(
                    createProcessUseCase, 
                    CreateProcessInputMapper.map(dto, timezoneId),
                    output -> {
                        Key<UUID> uuid = CreateProcessOutputMapper.map(output.getProcess());
                        return ResponseEntity.created(URI.create("/process/" + uuid.uuidValue())).body(new ApiResponse(true, "Process created successfully."));
                    }
            );
            
        } catch(InvalidDateException e) {
            throw new VeoException(VeoException.Error.ILLEGAL_ARGUMENTS, "Could not create process - illegal date given.");
        } catch (DomainException e) {
            throw new VeoException(VeoException.Error.UNKNOWN, "Could not create process.");
        }
    }
    
   
    

}
