/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.core.usecase.message;

import java.util.List;

import jakarta.validation.Valid;

import org.veo.core.entity.SystemMessage;
import org.veo.core.repository.SystemMessageRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GetAllSystemMessageUseCase
    implements TransactionalUseCase<
        GetAllSystemMessageUseCase.InputData, GetAllSystemMessageUseCase.OutputData> {

  final SystemMessageRepository systemMessageRepository;

  @Override
  public OutputData execute(InputData input) {
    List<SystemMessage> all = systemMessageRepository.findAll();
    return new OutputData(all);
  }

  @Valid
  public record InputData() implements UseCase.InputData {}

  @Valid
  public record OutputData(List<SystemMessage> messages) implements UseCase.OutputData {}
}
