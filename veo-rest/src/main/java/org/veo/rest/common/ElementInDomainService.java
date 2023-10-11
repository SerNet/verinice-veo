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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.validation.Valid;

import org.hibernate.Hibernate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainAssociationDto;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.AddLinksUseCase;
import org.veo.core.usecase.base.AssociateElementWithDomainUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UpdateElementInDomainUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.service.DbIdRefResolver;
import org.veo.core.usecase.service.IdRefResolver;
import org.veo.core.usecase.service.TypedId;
import org.veo.rest.TransactionalRunner;
import org.veo.rest.security.ApplicationUser;
import org.veo.service.EtagService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ElementInDomainService {
  private final ClientLookup clientLookup;
  private final RepositoryProvider repositoryProvider;
  private final EtagService etagService;
  private final UseCaseInteractor useCaseInteractor;
  private final AssociateElementWithDomainUseCase associateUseCase;
  private final ReferenceAssembler referenceAssembler;
  private final EvaluateElementUseCase evaluateElementUseCase;
  private final AddLinksUseCase addLinksUseCase;
  private final DomainRepository domainRepository;
  private final EntitySchemaService entitySchemaService;
  private final TransactionalRunner runner;
  private final CacheControl defaultCacheControl = CacheControl.noCache();

  public @Valid <
          TElement extends Element,
          TFullDto extends AbstractElementInDomainDto<TElement> & IdentifiableDto>
      Future<ResponseEntity<TFullDto>> getElement(
          Authentication auth,
          String domainId,
          String uuid,
          WebRequest request,
          Class<TElement> modelType,
          GetElementUseCase<TElement> getElementUseCase,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    var client = clientLookup.getClient(auth);
    if (etagService.getEtag(modelType, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    return useCaseInteractor
        .execute(
            getElementUseCase,
            new GetElementUseCase.InputData(Key.uuidFrom(uuid), client, Key.uuidFrom(domainId)),
            output -> toDtoMapper.apply(output.getElement(), output.getDomain()))
        .thenApply(dto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(dto));
  }

  public @Valid <TElement extends Element> boolean ensureElementExists(
      Client client, String domainId, String uuid, GetElementUseCase<TElement> getElementUseCase) {
    return runner.run(
            () ->
                getElementUseCase.execute(
                    new GetElementUseCase.InputData(
                        Key.uuidFrom(uuid), client, Key.uuidFrom(domainId))))
        != null;
  }

  public <
          TElement extends Element,
          TFullDto extends AbstractElementInDomainDto<TElement>,
          TInput extends GetElementsUseCase.InputData>
      Future<PageDto<TFullDto>> getElements(
          String domainId,
          GetElementsUseCase<TElement, TInput> getElementsUseCase,
          TInput input,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    return useCaseInteractor.execute(
        getElementsUseCase,
        input,
        output ->
            PagingMapper.toPage(
                output.getElements(),
                e ->
                    toDtoMapper.apply(
                        e,
                        e.getDomains().stream()
                            .filter(d -> d.getIdAsString().equals(domainId))
                            .findFirst()
                            .orElseThrow())));
  }

  public <TElement extends Element, TBaseDto extends AbstractElementInDomainDto<TElement>>
      CompletableFuture<ResponseEntity<ApiResponseBody>> createElement(
          ApplicationUser user,
          String domainId,
          TBaseDto dto,
          List<String> scopeIds,
          CreateElementUseCase<TElement> createUseCase) {
    dto.setDomain(TypedId.from(domainId, Domain.class));
    return useCaseInteractor.execute(
        createUseCase,
        CreateElementInputMapper.map(dto, clientLookup.getClient(user), scopeIds),
        output -> RestApiResponse.created(output.getEntity(), domainId, referenceAssembler));
  }

  public <TElement extends Element, TFullDto extends AbstractElementInDomainDto<TElement>>
      CompletableFuture<ResponseEntity<TFullDto>> associateElementWithDomain(
          Authentication auth,
          String domainId,
          String uuid,
          CreateDomainAssociationDto dto,
          Class<TElement> modelType,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    return useCaseInteractor.execute(
        associateUseCase,
        new AssociateElementWithDomainUseCase.InputData(
            clientLookup.getClient(auth),
            modelType,
            Key.uuidFrom(uuid),
            Key.uuidFrom(domainId),
            dto.getSubType(),
            dto.getStatus()),
        o -> ResponseEntity.ok().body(toDtoMapper.apply((TElement) o.getElement(), o.getDomain())));
  }

  public <
          TElement extends Element,
          TFullDto extends AbstractElementInDomainDto<TElement> & IdentifiableDto>
      CompletableFuture<ResponseEntity<TFullDto>> update(
          Authentication auth,
          String domainId,
          String eTag,
          String id,
          TFullDto dto,
          UpdateElementInDomainUseCase<TElement> updateUseCase,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    dto.applyResourceId(id);
    dto.setDomain(TypedId.from(domainId, Domain.class));
    return useCaseInteractor.execute(
        updateUseCase,
        (Supplier<UpdateElementInDomainUseCase.InputData<TElement>>)
            () -> {
              var user = ApplicationUser.authenticatedUser(auth.getPrincipal());
              var client = clientLookup.getClient(user);
              return new UpdateElementInDomainUseCase.InputData<TElement>(
                  Key.uuidFrom(id), dto, Key.uuidFrom(domainId), client, eTag, user.getUsername());
            },
        output ->
            toResponseEntity(
                output.getEntity(),
                toDtoMapper,
                output.getEntity().getDomains().stream()
                    .filter(d -> d.getIdAsString().equals(domainId))
                    .findFirst()
                    .orElseThrow()));
  }

  public @Valid <TElement extends Element, TDto extends AbstractElementInDomainDto<TElement>>
      CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
          Authentication auth,
          @Valid TDto dto,
          String domainId,
          BiFunction<TDto, IdRefResolver, TElement> toEntityMapper) {
    var client = clientLookup.getClient(auth);
    dto.setDomain(TypedId.from(domainId, Domain.class));
    var element =
        runner.run(
            () -> {
              var e = toEntityMapper.apply(dto, new DbIdRefResolver(repositoryProvider, client));
              if (e instanceof CompositeElement) {
                // initialize subtypeAspects field for PartCountProvider
                // TODO VEO-1569: remove this when the PartCountProvider uses a repository method
                var ce = (CompositeElement) e;
                ce.getParts()
                    .forEach(p -> Hibernate.initialize(((CompositeElement) p).getSubTypeAspects()));
              }
              return e;
            });
    return useCaseInteractor.execute(
        evaluateElementUseCase,
        new EvaluateElementUseCase.InputData(client, Key.uuidFrom(domainId), element),
        output -> ResponseEntity.ok().body(output));
  }

  public CompletableFuture<ResponseEntity<ApiResponseBody>> addLinks(
      Authentication auth,
      String domainId,
      String elementId,
      LinkMapDto links,
      Class<? extends Element> assetClass) {
    return useCaseInteractor.execute(
        addLinksUseCase,
        new AddLinksUseCase.InputData(
            Key.uuidFrom(elementId),
            assetClass,
            Key.uuidFrom(domainId),
            links.getCustomLinkStates(),
            clientLookup.getClient(auth)),
        out ->
            ResponseEntity.noContent()
                .eTag(ETag.from(elementId, out.getEntity().getVersion()))
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
      Authentication auth, String domainId, String elementType) {
    return CompletableFuture.supplyAsync(
        () -> {
          var domain =
              domainRepository.getActiveByIdWithElementTypeDefinitionsAndRiskDefinitions(
                  Key.uuidFrom(domainId), clientLookup.getClient(auth).getId());
          return ResponseEntity.ok().body(entitySchemaService.getSchema(elementType, domain));
        });
  }
}
