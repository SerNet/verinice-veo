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
package org.veo.rest.common;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.ActionDto;
import org.veo.adapter.presenter.api.dto.ControlImplementationDto;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainAssociationDto;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.response.ActionResultDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.presenter.api.response.InOrOutboundLinkDto;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.QueryCondition;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.usecase.GetAvailableActionsUseCase;
import org.veo.core.usecase.GetLinksByElementUseCase;
import org.veo.core.usecase.PerformActionUseCase;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.AddLinksUseCase;
import org.veo.core.usecase.base.AssociateElementWithDomainUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UpdateElementInDomainUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.compliance.GetControlImplementationsUseCase;
import org.veo.core.usecase.compliance.GetRequirementImplementationsByControlImplementationUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.rest.TransactionalRunner;
import org.veo.rest.security.ApplicationUser;
import org.veo.service.EtagService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ElementInDomainService {
  private final ClientLookup clientLookup;
  private final EtagService etagService;
  private final UseCaseInteractor useCaseInteractor;
  private final GetElementsUseCase getElementsUseCase;
  private final AssociateElementWithDomainUseCase associateUseCase;
  private final ReferenceAssembler referenceAssembler;
  private final GetLinksByElementUseCase getLinksByElementUseCase;
  private final EvaluateElementUseCase evaluateElementUseCase;
  private final AddLinksUseCase addLinksUseCase;
  private final DomainRepository domainRepository;
  private final EntitySchemaService entitySchemaService;
  private final GetAvailableActionsUseCase getAvailableActionsUseCase;
  private final GetControlImplementationsUseCase getControlImplementationsByControlUseCase;
  private final GetRequirementImplementationsByControlImplementationUseCase
      getRequirementImplementationsByControlImplementationUseCase;
  private final PerformActionUseCase performActionUseCase;
  private final TransactionalRunner runner;
  private final CacheControl defaultCacheControl = CacheControl.noCache();
  private final EntityToDtoTransformer entityToDtoTransformer;

  public @Valid <
          TElement extends Element,
          TFullDto extends AbstractElementInDomainDto<TElement> & IdentifiableDto>
      Future<ResponseEntity<TFullDto>> getElement(
          Authentication auth,
          UUID domainId,
          UUID uuid,
          WebRequest request,
          Class<TElement> modelType,
          GetElementUseCase<TElement> getElementUseCase,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    var client = clientLookup.getClient(auth);
    if (etagService
        .getEtag(modelType, uuid.toString())
        .map(request::checkNotModified)
        .orElse(false)) {
      return null;
    }
    return useCaseInteractor
        .execute(
            getElementUseCase,
            new GetElementUseCase.InputData(Key.from(uuid), client, Key.from(domainId)),
            output -> toDtoMapper.apply(output.element(), output.domain()))
        .thenApply(dto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(dto));
  }

  public @Valid <TElement extends Element> boolean ensureElementExists(
      Client client, UUID domainId, UUID uuid, GetElementUseCase<TElement> getElementUseCase) {
    return runner.run(
            () ->
                getElementUseCase.execute(
                    new GetElementUseCase.InputData(Key.from(uuid), client, Key.from(domainId))))
        != null;
  }

  public <TElement extends Element, TFullDto extends AbstractElementInDomainDto<TElement>>
      Future<PageDto<TFullDto>> getElements(
          UUID domainId,
          GetElementsUseCase.InputData input,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper,
          Class<TElement> type) {
    return getElements(
        domainId,
        input.withElementTypes(
            new QueryCondition<>(Set.of(EntityType.getSingularTermByType(type)))),
        (Element e, Domain d) -> toDtoMapper.apply((TElement) e, d));
  }

  public <TFullDto> CompletableFuture<PageDto<TFullDto>> getElements(
      UUID domainId,
      GetElementsUseCase.InputData input,
      BiFunction<Element, Domain, TFullDto> toDtoMapper) {
    return useCaseInteractor.execute(
        getElementsUseCase,
        input,
        output ->
            PagingMapper.toPage(
                output.elements(),
                e ->
                    toDtoMapper.apply(
                        e,
                        e.getDomains().stream()
                            .filter(d -> d.getIdAsUUID().equals(domainId))
                            .findFirst()
                            .orElseThrow())));
  }

  public <TElement extends Element, TBaseDto extends AbstractElementInDomainDto<TElement>>
      CompletableFuture<ResponseEntity<ApiResponseBody>> createElement(
          ApplicationUser user,
          UUID domainId,
          TBaseDto dto,
          List<UUID> scopeIds,
          CreateElementUseCase<TElement> createUseCase) {
    dto.setDomain(TypedId.from(domainId, Domain.class));
    return useCaseInteractor.execute(
        createUseCase,
        CreateElementInputMapper.map(dto, clientLookup.getClient(user), scopeIds),
        output -> RestApiResponse.created(output.entity(), domainId, referenceAssembler));
  }

  public <TElement extends Element, TFullDto extends AbstractElementInDomainDto<TElement>>
      CompletableFuture<ResponseEntity<TFullDto>> associateElementWithDomain(
          Authentication auth,
          UUID domainId,
          UUID uuid,
          CreateDomainAssociationDto dto,
          Class<TElement> modelType,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    return useCaseInteractor.execute(
        associateUseCase,
        new AssociateElementWithDomainUseCase.InputData(
            clientLookup.getClient(auth),
            modelType,
            Key.from(uuid),
            Key.from(domainId),
            dto.getSubType(),
            dto.getStatus()),
        o -> ResponseEntity.ok().body(toDtoMapper.apply((TElement) o.element(), o.domain())));
  }

  public <
          TElement extends Element,
          TFullDto extends AbstractElementInDomainDto<TElement> & IdentifiableDto>
      CompletableFuture<ResponseEntity<TFullDto>> update(
          Authentication auth,
          UUID domainId,
          String eTag,
          UUID id,
          TFullDto dto,
          UpdateElementInDomainUseCase<TElement> updateUseCase,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    dto.applyResourceId(id);
    dto.setDomain(TypedId.from(domainId, Domain.class));
    var user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    var client = clientLookup.getClient(user);
    return useCaseInteractor.execute(
        updateUseCase,
        new UpdateElementInDomainUseCase.InputData<>(
            Key.from(id), dto, Key.from(domainId), client, eTag, user.getUsername()),
        output ->
            toResponseEntity(
                output.entity(),
                toDtoMapper,
                output.entity().getDomains().stream()
                    .filter(d -> d.getIdAsUUID().equals(domainId))
                    .findFirst()
                    .orElseThrow()));
  }

  public @Valid <TElement extends Element, TDto extends AbstractElementInDomainDto<TElement>>
      CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
          Authentication auth, @Valid TDto dto, UUID domainId) {
    dto.setDomain(TypedId.from(domainId, Domain.class));
    return useCaseInteractor.execute(
        evaluateElementUseCase,
        new EvaluateElementUseCase.InputData(clientLookup.getClient(auth), Key.from(domainId), dto),
        output -> ResponseEntity.ok().body(output));
  }

  public CompletableFuture<ResponseEntity<ApiResponseBody>> addLinks(
      Authentication auth,
      UUID domainId,
      UUID elementId,
      LinkMapDto links,
      Class<? extends Element> assetClass) {
    return useCaseInteractor.execute(
        addLinksUseCase,
        new AddLinksUseCase.InputData(
            Key.from(elementId),
            assetClass,
            Key.from(domainId),
            links.getCustomLinkStates(),
            clientLookup.getClient(auth)),
        out ->
            ResponseEntity.noContent()
                .eTag(ETag.from(elementId.toString(), out.entity().getVersion()))
                .build());
  }

  private <TElement extends Element, TFullDto extends AbstractElementInDomainDto<TElement>>
      ResponseEntity<TFullDto> toResponseEntity(
          TElement entity, BiFunction<TElement, Domain, TFullDto> toDtoMapper, Domain domain) {
    return ResponseEntity.ok()
        .eTag(ETag.from(entity.getIdAsString(), entity.getVersion()))
        .body(toDtoMapper.apply(entity, domain));
  }

  public CompletableFuture<ResponseEntity<String>> getJsonSchema(
      Authentication auth, UUID domainId, String elementType) {
    return CompletableFuture.supplyAsync(
        () -> {
          var domain =
              domainRepository.getActiveByIdWithElementTypeDefinitionsAndRiskDefinitions(
                  Key.from(domainId), clientLookup.getClient(auth).getId());
          return ResponseEntity.ok().body(entitySchemaService.getSchema(elementType, domain));
        });
  }

  public CompletableFuture<ResponseEntity<Set<ActionDto>>> getActions(
      UUID domainId, UUID uuid, Class<? extends Element> type, Authentication auth) {
    return useCaseInteractor.execute(
        getAvailableActionsUseCase,
        new GetAvailableActionsUseCase.InputData(
            Key.from(domainId), Key.from(uuid), type, clientLookup.getClient(auth).getId()),
        o -> ResponseEntity.ok(entityToDtoTransformer.transformActions2Dtos(o.actions())));
  }

  public CompletableFuture<ResponseEntity<ActionResultDto>> performAction(
      UUID domainId,
      UUID uuid,
      Class<? extends Element> elementType,
      String actionId,
      Authentication auth) {
    return useCaseInteractor.execute(
        performActionUseCase,
        new PerformActionUseCase.InputData(
            Key.from(domainId),
            Key.from(uuid),
            elementType,
            actionId,
            clientLookup.getClient(auth).getId()),
        o -> ResponseEntity.ok(entityToDtoTransformer.transformActionResult2Dto(o.result())));
  }

  public <TElement extends Element>
      Future<PageDto<ControlImplementationDto>> getControlImplementations(
          UUID domainId, GetControlImplementationsUseCase.InputData input) {
    return useCaseInteractor.execute(
        getControlImplementationsByControlUseCase,
        input,
        output ->
            PagingMapper.toPage(
                output.page(),
                e ->
                    entityToDtoTransformer.mapControlImplementation(
                        e,
                        e.getOwner().getDomains().stream()
                            .filter(d -> d.getIdAsUUID().equals(domainId))
                            .findFirst()
                            .orElseThrow())));
  }

  public Future<PageDto<RequirementImplementationDto>> getRequirementImplementations(
      GetRequirementImplementationsByControlImplementationUseCase.InputData input,
      List<String> controlCustomAspectKeys) {

    return useCaseInteractor.execute(
        getRequirementImplementationsByControlImplementationUseCase,
        input,
        out ->
            PagingMapper.toPage(
                out.result(),
                o -> {
                  var domain = out.domain();
                  if (controlCustomAspectKeys != null) {
                    List<String> availableCAs =
                        domain
                            .getElementTypeDefinition(Control.SINGULAR_TERM)
                            .getCustomAspects()
                            .keySet()
                            .stream()
                            .sorted()
                            .toList();
                    List<String> invalidCas =
                        controlCustomAspectKeys.stream()
                            .filter(Predicate.not(availableCAs::contains))
                            .sorted()
                            .toList();
                    if (!invalidCas.isEmpty()) {
                      throw new NotFoundException(
                          "Invalid custom aspect ID(s): %s, available aspects: %s",
                          invalidCas, availableCAs);
                    }
                  }
                  return entityToDtoTransformer.transformRequirementImplementation2Dto(
                      o, domain, controlCustomAspectKeys);
                }));
  }

  public CompletableFuture<ResponseEntity<PageDto<InOrOutboundLinkDto>>> getLinks(
      Authentication auth,
      UUID domainId,
      UUID uuid,
      Class<? extends Element> elementType,
      @Min(1) Integer pageSize,
      Integer pageNumber,
      LinkQuery.SortCriterion sortColumn,
      String sortOrder) {
    return useCaseInteractor
        .execute(
            getLinksByElementUseCase,
            new GetLinksByElementUseCase.InputData(
                clientLookup.getClient(auth),
                TypedId.from(uuid, elementType),
                TypedId.from(domainId, Domain.class),
                PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)),
            p ->
                PagingMapper.toPage(
                    p.page(),
                    link -> InOrOutboundLinkDto.from(link, p.domain(), referenceAssembler)))
        .thenApply(ResponseEntity::ok);
  }
}
