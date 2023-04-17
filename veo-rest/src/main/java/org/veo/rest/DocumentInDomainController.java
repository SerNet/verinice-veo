/*******************************************************************************
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
 ******************************************************************************/
package org.veo.rest;

import static org.veo.rest.ControllerConstants.CHILD_ELEMENT_IDS_PARAM;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.HAS_CHILD_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.HAS_PARENT_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.NAME_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SCOPE_IDS_DESCRIPTION;
import static org.veo.rest.ControllerConstants.SCOPE_IDS_PARAM;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PATTERN;
import static org.veo.rest.ControllerConstants.STATUS_PARAM;
import static org.veo.rest.ControllerConstants.SUB_TYPE_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UPDATED_BY_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;
import static org.veo.rest.ControllerConstants.UUID_PARAM_SPEC;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.create.CreateDocumentInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentInDomainDto;
import org.veo.adapter.presenter.api.io.mapper.GetElementsInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Document;
import org.veo.core.usecase.base.UpdateDocumentInDomainUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.document.CreateDocumentUseCase;
import org.veo.core.usecase.document.GetDocumentUseCase;
import org.veo.core.usecase.document.GetDocumentsUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.ClientLookup;
import org.veo.rest.common.ElementInDomainService;
import org.veo.rest.schemas.EvaluateElementOutputSchema;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** REST service which provides methods to manage documents from the viewpoint of a domain. */
@RequiredArgsConstructor
@RestController
@RequestMapping(DocumentInDomainController.URL_BASE_PATH)
@Slf4j
public class DocumentInDomainController {
  // TODO VEO-2000 replace /domians with Domain.PLURAL_TERM
  public static final String URL_BASE_PATH = "/domians/{domainId}/" + Document.PLURAL_TERM;
  private final ClientLookup clientLookup;
  private final GetDocumentUseCase getDocumentUseCase;
  private final GetDocumentsUseCase getDocumentsUseCase;
  private final CreateDocumentUseCase createUseCase;
  private final UpdateDocumentInDomainUseCase updateUseCase;
  private final ElementInDomainService elementService;
  private final DtoToEntityTransformer dtoToEntityTransformer;
  private final EntityToDtoTransformer entityToDtoTransformer;

  @Operation(summary = "Loads a document from the viewpoint of a domain")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Document loaded",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FullDocumentInDomainDto.class))),
    @ApiResponse(responseCode = "404", description = "Document not found"),
    @ApiResponse(responseCode = "404", description = "Domain not found"),
    @ApiResponse(responseCode = "404", description = "Document not associated with domain"),
  })
  @GetMapping(UUID_PARAM_SPEC)
  public @Valid Future<ResponseEntity<FullDocumentInDomainDto>> getElement(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return elementService.getElement(
        auth,
        domainId,
        uuid,
        request,
        Document.class,
        getDocumentUseCase,
        entityToDtoTransformer::transformDocument2Dto);
  }

  @GetMapping
  @Operation(summary = "Loads all documents in a domain")
  public @Valid Future<PageDto<FullDocumentInDomainDto>> getDocument(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = STATUS_PARAM, required = false) String status,
      @RequestParam(value = CHILD_ELEMENT_IDS_PARAM, required = false) List<String> childElementIds,
      @RequestParam(value = HAS_PARENT_ELEMENTS_PARAM, required = false) Boolean hasParentElements,
      @RequestParam(value = HAS_CHILD_ELEMENTS_PARAM, required = false) Boolean hasChildElements,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(
              value = SORT_COLUMN_PARAM,
              required = false,
              defaultValue = SORT_COLUMN_DEFAULT_VALUE)
          String sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder) {
    return elementService.getElements(
        domainId,
        getDocumentsUseCase,
        GetElementsInputMapper.map(
            clientLookup.getClient(auth),
            unitUuid,
            domainId,
            displayName,
            subType,
            status,
            childElementIds,
            hasChildElements,
            hasParentElements,
            description,
            designator,
            name,
            updatedBy,
            PagingMapper.toConfig(
                pageSize, pageNumber,
                sortColumn, sortOrder)),
        entityToDtoTransformer::transformDocument2Dto);
  }

  @Operation(summary = "Creates a document, assigning it to the domain")
  @PostMapping
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Document created",
            headers = @Header(name = "Location")),
        @ApiResponse(responseCode = "404", description = "Domain not found"),
      })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createElement(
      @Parameter(required = true, hidden = true) ApplicationUser user,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Valid @NotNull @RequestBody CreateDocumentInDomainDto dto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<String> scopeIds) {
    return elementService.createElement(
        user,
        domainId,
        dto,
        scopeIds,
        createUseCase,
        dtoToEntityTransformer::transformDto2Document);
  }

  @Operation(summary = "Updates a document from the viewpoint of a domain")
  @PutMapping(UUID_PARAM_SPEC)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Document updated"),
    @ApiResponse(responseCode = "404", description = "Document not found"),
    @ApiResponse(responseCode = "404", description = "Document not associated with domain"),
  })
  public CompletableFuture<ResponseEntity<FullDocumentInDomainDto>> updateElement(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      @Valid @NotNull @RequestBody FullDocumentInDomainDto dto) {
    return elementService.update(
        auth,
        domainId,
        eTag,
        uuid,
        dto,
        updateUseCase,
        dtoToEntityTransformer::transformDto2Document,
        entityToDtoTransformer::transformDocument2Dto);
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient document without persisting anything")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Element evaluated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = EvaluateElementOutputSchema.class))),
    @ApiResponse(responseCode = "404", description = "Domain not found")
  })
  @PostMapping(value = "/evaluation")
  public @Valid CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Valid @RequestBody FullDocumentInDomainDto dto) {
    return elementService.evaluate(
        auth, dto, domainId, dtoToEntityTransformer::transformDto2Document);
  }
}