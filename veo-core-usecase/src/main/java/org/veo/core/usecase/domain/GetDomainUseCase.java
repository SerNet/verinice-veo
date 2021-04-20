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
package org.veo.core.usecase.domain;

import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

public class GetDomainUseCase
        implements TransactionalUseCase<GetDomainUseCase.InputData, GetDomainUseCase.OutputData> {
    private final DomainRepository repository;

    public GetDomainUseCase(DomainRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutputData execute(InputData input) {
        Domain domain = repository.findById(input.getId())
                                  .orElseThrow(() -> new NotFoundException(input.getId()
                                                                                .uuidValue()));
        if (!input.authenticatedClient.equals(domain.getOwner())) {
            throw new ClientBoundaryViolationException(
                    "The domain is not accessable from this client.");
        }
        if (!domain.isActive()) {
            throw new NotFoundException("Domain is inactive.");
        }
        return new OutputData(domain);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Key<UUID> id;
        Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Domain domain;
    }
}
