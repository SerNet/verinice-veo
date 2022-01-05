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
package org.veo.core.usecase.domaintemplate;

import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

public class CreateDomainTemplateFromDomainUseCase implements
        TransactionalUseCase<CreateDomainTemplateFromDomainUseCase.InputData, CreateDomainTemplateFromDomainUseCase.OutputData> {
    private final DomainTemplateService domainTemplateService;
    private final DomainRepository repository;
    private final DomainTemplateRepository domainTemplateRepository;

    public CreateDomainTemplateFromDomainUseCase(DomainTemplateService domainTemplateService,
            DomainRepository repository, DomainTemplateRepository domainTemplateRepository) {
        super();
        this.domainTemplateService = domainTemplateService;
        this.repository = repository;
        this.domainTemplateRepository = domainTemplateRepository;
    }

    @Override
    public OutputData execute(InputData input) {
        Domain domain = repository.findById(input.getId())
                                  .orElseThrow(() -> new NotFoundException(
                                          "Domain with id %s not found.", input.getId()));
        Client client = input.getAuthenticatedClient();
        if (!client.equals(domain.getOwner())) {
            throw new ClientBoundaryViolationException(domain, client);
        }
        if (!domain.isActive()) {
            throw new NotFoundException("Domain is inactive.");
        }

        domain = updateRevision(domain, input.revision);
        DomainTemplate domainTemplateFromDomain = domainTemplateService.createDomainTemplateFromDomain(domain);
        return new OutputData(domainTemplateRepository.save(domainTemplateFromDomain));
    }

    /**
     * Increase the revision field if the content is a number.
     */
    private Domain updateRevision(Domain domain, String revision) {
        domain.setRevision(revision);
        return repository.save(domain);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Key<UUID> id;
        String revision;
        Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        DomainTemplate newDomainTemplate;
    }

}
