/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.usecase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.InOrOutboundLink;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetLinksByElementUseCase
    implements TransactionalUseCase<
        GetLinksByElementUseCase.InputData, GetLinksByElementUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final GenericElementRepository elementRepository;

  @Override
  public OutputData execute(InputData input) {
    var domain =
        domainRepository.getById(input.domainRef.toKey(), input.authenticatedClient.getId());
    var element =
        elementRepository.getById(
            input.elementRef().toKey(), input.elementRef.getType(), input.authenticatedClient);
    if (!element.isAssociatedWithDomain(domain)) {
      throw NotFoundException.elementNotAssociatedWithDomain(element, domain.getIdAsString());
    }
    var query = elementRepository.queryLinks(element, domain);
    return new OutputData(query.execute(input.pagingConfiguration), domain);
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      @NotNull TypedId<? extends Element> elementRef,
      @NotNull TypedId<Domain> domainRef,
      @NotNull PagingConfiguration<LinkQuery.SortCriterion> pagingConfiguration)
      implements UseCase.InputData {}

  public record OutputData(
      @Valid PagedResult<InOrOutboundLink, LinkQuery.SortCriterion> page, Domain domain)
      implements UseCase.OutputData {}
}
