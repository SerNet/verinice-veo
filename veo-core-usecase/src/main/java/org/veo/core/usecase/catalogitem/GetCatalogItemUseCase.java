/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
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
package org.veo.core.usecase.catalogitem;

import javax.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;
import org.veo.core.usecase.UseCaseTools;

import lombok.Value;

public class GetCatalogItemUseCase
        implements TransactionalUseCase<IdAndClient, GetCatalogItemUseCase.OutputData> {
    private final CatalogItemRepository repository;

    public GetCatalogItemUseCase(CatalogItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutputData execute(IdAndClient input) {
        CatalogItem catalogItem = repository.findById(input.getId())
                                            .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                          .uuidValue()));

        DomainTemplate domaintemplate = catalogItem.getCatalog()
                                                   .getDomainTemplate();
        UseCaseTools.checkDomainBelongsToClient(input.getAuthenticatedClient(), domaintemplate);
        if (!((Domain) domaintemplate).isActive()) {
            throw new NotFoundException("Domain is inactive.");
        }
        return new OutputData(catalogItem);
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        CatalogItem catalogItem;
    }
}
