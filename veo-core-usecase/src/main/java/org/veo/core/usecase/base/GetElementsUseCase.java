/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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
package org.veo.core.usecase.base;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.CompositeElementQuery;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ElementQueryProvider;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.SingleValueQueryCondition;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.With;

/** Reinstantiate persisted entity objects. */
@RequiredArgsConstructor
public class GetElementsUseCase
    implements TransactionalUseCase<GetElementsUseCase.InputData, GetElementsUseCase.OutputData> {

  private final ClientRepository clientRepository;
  private final GenericElementRepository genericRepository;
  private final RepositoryProvider repositoryProvider;
  private final UnitRepository unitRepository;

  /**
   * Find persisted control objects and reinstantiate them. Throws a domain exception if the
   * (optional) requested parent unit was not found in the repository.
   */
  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    Client client =
        UseCaseTools.checkClientExists(userAccessRights.getClientId(), clientRepository);

    var query = getRepo(input.elementTypes).query(client);
    applyDefaultQueryParameters(input, query, client, userAccessRights);
    return new OutputData(query.execute(input.pagingConfiguration));
  }

  private ElementQueryProvider<? extends Element> getRepo(
      QueryCondition<ElementType> elementTypes) {
    // Prefer specific repository, because it supports more query filters.
    // TODO #2902 fix filters with generic repo
    if (elementTypes != null && elementTypes.getValues().size() == 1) {
      return repositoryProvider.getElementRepositoryFor(
          elementTypes.getValues().iterator().next().getType());
    }
    return genericRepository;
  }

  protected void applyDefaultQueryParameters(
      InputData input, ElementQuery<?> query, Client client, UserAccessRights userAccessRights) {

    Optional.ofNullable(input.elementTypes).ifPresent(query::whereElementTypeMatches);
    // TODO: verinice-veo#3950
    if (userAccessRights.isUnitAccessRestricted()) {
      Optional.ofNullable(new QueryCondition<>(userAccessRights.getReadableUnitIds()))
          .map(c -> unitRepository.findByIds(c.getValues()))
          .ifPresent(query::whereUnitIn);
    }
    Optional.ofNullable(input.unitUuid)
        .map(
            condition ->
                condition.getValues().stream()
                    .map(unitRepository::getById)
                    .collect(Collectors.toSet()))
        .ifPresent(query::whereUnitIn);

    Optional.ofNullable(input.domainId)
        .map(
            condition ->
                client.getDomains().stream()
                    .filter(d -> d.getId().equals(condition.getValue()))
                    .findFirst()
                    .orElseThrow(
                        () -> new NotFoundException(input.domainId.getValue(), Domain.class)))
        .ifPresentOrElse(
            d ->
                Optional.ofNullable(input.subType)
                    .ifPresentOrElse(
                        s -> query.whereSubTypeMatches(s, d), () -> query.whereDomainsContain(d)),
            () -> Optional.ofNullable(input.subType).ifPresent(query::whereSubTypeMatches));

    Optional.ofNullable(input.status).ifPresent(query::whereStatusMatches);
    Optional.ofNullable(input.displayName).ifPresent(query::whereDisplayNameMatchesIgnoringCase);
    Optional.ofNullable(input.description).ifPresent(query::whereDescriptionMatchesIgnoreCase);
    Optional.ofNullable(input.designator).ifPresent(query::whereDesignatorMatchesIgnoreCase);
    Optional.ofNullable(input.name).ifPresent(query::whereNameMatchesIgnoreCase);
    Optional.ofNullable(input.abbreviation).ifPresent(query::whereAbbreviationMatchesIgnoreCase);
    Optional.ofNullable(input.updatedBy).ifPresent(query::whereUpdatedByContainsIgnoreCase);
    Optional.ofNullable(input.childElementIds).ifPresent(query::whereChildElementIn);
    Optional.ofNullable(input.hasChildElements)
        .map(SingleValueQueryCondition::getValue)
        .ifPresent(query::whereChildElementsPresent);
    Optional.ofNullable(input.hasParentElements)
        .map(SingleValueQueryCondition::getValue)
        .ifPresent(query::whereParentElementPresent);
    Optional.ofNullable(input.compositeId)
        .ifPresent(
            condition -> {
              if (query instanceof CompositeElementQuery<?> c) {
                c.whereCompositesContain(condition);
              } else {
                throw new IllegalArgumentException(
                    "Composite filter not compatible with query type");
              }
            });
    Optional.ofNullable(input.scopeId).ifPresent(query::whereScopesContain);
    query.fetchChildren();
    if (input.embedRisks) {
      query.fetchRisks();
    }
    query.fetchControlImplementations();
    query.fetchRequirementImplementations();
    query.fetchRiskValuesAspects();
  }

  @Valid
  @Builder
  @With
  public record InputData(
      QueryCondition<ElementType> elementTypes,
      QueryCondition<UUID> unitUuid,
      SingleValueQueryCondition<UUID> domainId,
      QueryCondition<String> displayName,
      QueryCondition<String> subType,
      QueryCondition<String> status,
      QueryCondition<UUID> childElementIds,
      SingleValueQueryCondition<Boolean> hasChildElements,
      SingleValueQueryCondition<Boolean> hasParentElements,
      SingleValueQueryCondition<UUID> compositeId,
      SingleValueQueryCondition<UUID> scopeId,
      QueryCondition<String> description,
      QueryCondition<String> designator,
      QueryCondition<String> name,
      QueryCondition<String> abbreviation,
      QueryCondition<String> updatedBy,
      PagingConfiguration<String> pagingConfiguration,
      boolean embedRisks)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid PagedResult<? extends Element, String> elements)
      implements UseCase.OutputData {}
}
