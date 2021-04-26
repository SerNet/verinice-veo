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
package org.veo.core.usecase.domaintemplate;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.repository.ClientRepository;

import lombok.Value;

public class GetDomainTemplateUseCase
        implements TransactionalUseCase<IdAndClient, GetDomainTemplateUseCase.OutputData> {
    private final DomainTemplateService templateService;
    private final ClientRepository clientRepository;

    public GetDomainTemplateUseCase(DomainTemplateService templateService,
            ClientRepository clientRepository) {
        this.templateService = templateService;
        this.clientRepository = clientRepository;
    }

    @Override
    public OutputData execute(IdAndClient input) {
        Client client = UseCaseTools.checkClientExists(input.getAuthenticatedClient()
                                                            .getId(),
                                                       clientRepository);

        DomainTemplate domainTemplate = templateService.getTemplate(client, input.getId())
                                                       .orElseThrow(() -> new NotFoundException(
                                                               "Invalid domain template"));
        return new OutputData(domainTemplate);
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        DomainTemplate domainTemplate;
    }
}
