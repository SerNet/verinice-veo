/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
import static org.veo.rest.ControllerConstants.CHILD_ELEMENT_IDS_PARAM;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.DOMAIN_PARAM;
import static org.veo.rest.ControllerConstants.HAS_CHILD_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.HAS_PARENT_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.NAME_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
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
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM_SPEC;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Document;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.document.GetDocumentUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.schemas.EvaluateElementOutputSchema;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

/** REST service which provides methods to manage documents. */
@RestController
@RequestMapping(DocumentController.URL_BASE_PATH)
@Slf4j
public class DocumentController
    extends AbstractCompositeElementController<Document, FullDocumentDto> {

  public DocumentController(
      GetDocumentUseCase getDocumentUseCase,
      GetElementsUseCase getElementsUseCase,
      DeleteElementUseCase deleteElementUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase) {
    super(
        ElementType.DOCUMENT,
        getDocumentUseCase,
        evaluateElementUseCase,
        inspectElementUseCase,
        getElementsUseCase);
    this.deleteElementUseCase = deleteElementUseCase;
  }

  public static final String URL_BASE_PATH = "/" + Document.PLURAL_TERM;

  private final DeleteElementUseCase deleteElementUseCase;

  @GetMapping
  @Operation(summary = "Loads all documents")
  public @Valid Future<PageDto<FullDocumentDto>> getDocuments(
      @Parameter(hidden = true) Authentication auth,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) UUID unitUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = STATUS_PARAM, required = false) String status,
      @RequestParam(value = CHILD_ELEMENT_IDS_PARAM, required = false) List<UUID> childElementIds,
      @RequestParam(value = HAS_PARENT_ELEMENTS_PARAM, required = false) Boolean hasParentElements,
      @RequestParam(value = HAS_CHILD_ELEMENTS_PARAM, required = false) Boolean hasChildElements,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = ABBREVIATION_PARAM, required = false) String abbreviation,
      @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
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
    Client client = getAuthenticatedClient(auth);

    return getElements(
        QueryInputMapper.map(
            client,
            unitUuid,
            null,
            displayName,
            subType,
            status,
            childElementIds,
            hasChildElements,
            hasParentElements,
            null,
            null,
            description,
            designator,
            name,
            abbreviation,
            updatedBy,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)));
  }

  @Override
  @Operation(summary = "Loads a document")
  @ApiResponse(
      responseCode = "200",
      description = "Document loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullDocumentDto.class)))
  @ApiResponse(responseCode = "404", description = "Document not found")
  @GetMapping(ControllerConstants.UUID_PARAM_SPEC)
  public Future<ResponseEntity<FullDocumentDto>> getElement(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    return super.getElement(auth, uuid, request);
  }

  @Override
  @Operation(summary = "Loads the parts of a document")
  @ApiResponse(
      responseCode = "200",
      description = "Parts loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullDocumentDto.class))))
  @ApiResponse(responseCode = "404", description = "Document not found")
  @GetMapping(value = "/{" + UUID_PARAM + "}/parts")
  public CompletableFuture<ResponseEntity<List<FullDocumentDto>>> getElementParts(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    return super.getElementParts(auth, uuid, request);
  }

  @DeleteMapping(ControllerConstants.UUID_PARAM_SPEC)
  @Operation(summary = "Deletes a document")
  @ApiResponse(responseCode = "204", description = "Document deleted")
  @ApiResponse(responseCode = "404", description = "Document not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteDocument(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Client client = getClient(user.getClientId());
    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(Document.class, uuid, client),
        output -> ResponseEntity.noContent().build());
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient document without persisting anything")
  @ApiResponse(
      responseCode = "200",
      description = "Element evaluated",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = EvaluateElementOutputSchema.class)))
  @PostMapping(value = "/evaluation")
  @Override
  public CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Valid @RequestBody FullDocumentDto element,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(auth, element, domainId);
  }

  @Operation(summary = "Runs inspections on a persisted document")
  @ApiResponse(
      responseCode = "200",
      description = "Inspections have run",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Finding.class))))
  @ApiResponse(responseCode = "404", description = "Document not found")
  @GetMapping(value = UUID_PARAM_SPEC + "/inspection")
  public @Valid CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(value = DOMAIN_PARAM) UUID domainId) {
    return inspect(auth, uuid, domainId, Document.class);
  }

  @Override
  protected FullDocumentDto entity2Dto(Document entity) {
    return entityToDtoTransformer.transformDocument2Dto(entity, false);
  }
}
