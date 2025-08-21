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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.SystemMessageState;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.SystemMessageRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.service.EntityStateMapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SaveSystemMessageUseCase
    implements TransactionalUseCase<
        SaveSystemMessageUseCase.InputData, SaveSystemMessageUseCase.OutputData> {

  final SystemMessageRepository systemMessageRepository;
  final EntityFactory entityFactory;
  final EntityStateMapper entityStateMapper;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var message =
        input.id() == null
            ? entityFactory.createSystemMessage()
            : systemMessageRepository.getById(input.id());

    entityStateMapper.mapSystemMessage(input.message(), message);
    if (message.getPublication() == null) {
      message.setPublication(Instant.now().plus(1, ChronoUnit.SECONDS));
    }
    validate(message);
    message = systemMessageRepository.save(message);
    return new OutputData(message.getId(), input.id() == null);
  }

  private void validate(SystemMessage message) {
    if (message
        .getPublication()
        .isBefore(message.getCreatedAt() == null ? Instant.now() : message.getCreatedAt()))
      throw new UnprocessableDataException("Publication time is before creation time.");
    if (message.getEffective() != null && message.getEffective().isBefore(message.getPublication()))
      throw new UnprocessableDataException("Effective time is before publication time.");
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(Long id, SystemMessageState message) implements UseCase.InputData {}

  @Valid
  public record OutputData(Long id, boolean created) implements UseCase.OutputData {}
}
