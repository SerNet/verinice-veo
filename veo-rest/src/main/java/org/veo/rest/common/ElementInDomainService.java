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

import javax.validation.Valid;

import org.apache.commons.lang3.function.TriFunction;
import org.hibernate.Hibernate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.DbIdRefResolver;
import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UpdateElementInDomainUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
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
  private final ReferenceAssembler referenceAssembler;
  private final EvaluateElementUseCase evaluateElementUseCase;
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
          CreateElementUseCase<TElement> createUseCase,
          TriFunction<TBaseDto, String, IdRefResolver, TElement> toEntityMapper) {
    return useCaseInteractor.execute(
        createUseCase,
        (Supplier<CreateElementUseCase.InputData<TElement>>)
            () -> {
              var client = clientLookup.getClient(user);
              return CreateElementInputMapper.map(
                  toEntityMapper.apply(
                      dto, domainId, new DbIdRefResolver(repositoryProvider, client)),
                  client,
                  scopeIds);
            },
        output -> RestApiResponse.created(output.getEntity(), domainId, referenceAssembler));
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
          TriFunction<TFullDto, String, IdRefResolver, TElement> toEntityMapper,
          BiFunction<TElement, Domain, TFullDto> toDtoMapper) {
    dto.applyResourceId(id);
    return useCaseInteractor.execute(
        updateUseCase,
        (Supplier<UpdateElementInDomainUseCase.InputData<TElement>>)
            () -> {
              var user = ApplicationUser.authenticatedUser(auth.getPrincipal());
              var client = clientLookup.getClient(user);
              var idRefResolver = new DbIdRefResolver(repositoryProvider, client);
              return new UpdateElementInDomainUseCase.InputData<>(
                  toEntityMapper.apply(dto, domainId, idRefResolver),
                  Key.uuidFrom(domainId),
                  client,
                  eTag,
                  user.getUsername());
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
          TriFunction<TDto, String, IdRefResolver, TElement> toEntityMapper) {
    var client = clientLookup.getClient(auth);
    var element =
        runner.run(
            () -> {
              var e =
                  toEntityMapper.apply(
                      dto, domainId, new DbIdRefResolver(repositoryProvider, client));
              if (e instanceof CompositeElement) {
                // initialize subtypeAspects field for PartCountProvider
                // TODO VEO-1569: remove this when the PartCountProvider uses a repository method
                var ce = (CompositeElement<CompositeElement>) e;
                ce.getParts().forEach(p -> Hibernate.initialize(p.getSubTypeAspects()));
              }
              return e;
            });
    return useCaseInteractor.execute(
        evaluateElementUseCase,
        new EvaluateElementUseCase.InputData(client, Key.uuidFrom(domainId), element),
        output -> ResponseEntity.ok().body(output));
  }

  private <TElement extends Element, TFullDto extends AbstractElementInDomainDto<TElement>>
      ResponseEntity<TFullDto> toResponseEntity(
          TElement entity, BiFunction<TElement, Domain, TFullDto> toDtoMapper, Domain domain) {
    return ResponseEntity.ok()
        .eTag(ETag.from(entity.getIdAsString(), entity.getVersion()))
        .body(toDtoMapper.apply(entity, domain));
  }
}