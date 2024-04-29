/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import org.veo.core.entity.DomainTemplate;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindDomainTemplatesUseCase
    implements TransactionalUseCase<UseCase.EmptyInput, FindDomainTemplatesUseCase.OutputData> {
  private final DomainTemplateRepository repository;

  @Override
  public OutputData execute(EmptyInput input) {
    return new OutputData(repository.findAll());
  }

  @Valid
  public record OutputData(@Valid List<DomainTemplate> getDomainTemplates)
      implements UseCase.OutputData {}
}
