/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.usecase.domain;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;

import lombok.Value;

public class GetDomainUseCase
        implements TransactionalUseCase<IdAndClient, GetDomainUseCase.OutputData> {
    private final DomainRepository repository;

    public GetDomainUseCase(DomainRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutputData execute(IdAndClient input) {
        Domain domain = repository.findById(input.getId())
                                  .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                .uuidValue()));
        Client client = input.getAuthenticatedClient();
        if (!client.equals(domain.getOwner())) {
            throw new ClientBoundaryViolationException(domain, client);
        }
        if (!domain.isActive()) {
            throw new NotFoundException("Domain is inactive.");
        }
        return new OutputData(domain);
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Domain domain;
    }
}
