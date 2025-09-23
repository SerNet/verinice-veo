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

import java.util.List;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.repository.ClientRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;

public class GetDomainTemplatesUseCase
    implements TransactionalUseCase<UseCase.EmptyInput, GetDomainTemplatesUseCase.OutputData> {

  private final ClientRepository clientRepository;
  private final DomainTemplateService templateService;

  public GetDomainTemplatesUseCase(
      DomainTemplateService templateService, ClientRepository clientRepository) {
    this.clientRepository = clientRepository;
    this.templateService = templateService;
  }

  /**
   * Find persisted control objects and reinstantiate them. Throws a domain exception if the
   * (optional) requested parent unit was not found in the repository.
   */
  @Override
  public OutputData execute(EmptyInput input, UserAccessRights userAccessRights) {
    Client client =
        UseCaseTools.checkClientExists(userAccessRights.getClientId(), clientRepository);

    return new OutputData(templateService.getTemplates(client));
  }

  @Valid
  public record OutputData(@Valid List<DomainTemplate> objects) implements UseCase.OutputData {}
}
