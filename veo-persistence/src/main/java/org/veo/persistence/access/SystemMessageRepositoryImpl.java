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
package org.veo.persistence.access;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.SystemMessageRepository;
import org.veo.persistence.access.jpa.SystemMessageDataRepository;
import org.veo.persistence.entity.jpa.SystemMessageData;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class SystemMessageRepositoryImpl implements SystemMessageRepository {
  private final SystemMessageDataRepository dataRepository;

  @Override
  public SystemMessage save(SystemMessage entity) {
    return dataRepository.save((SystemMessageData) entity);
  }

  @Override
  public void delete(SystemMessage entity) {
    dataRepository.delete((SystemMessageData) entity);
  }

  @Override
  public List<SystemMessage> findAll() {
    return dataRepository.findAll().stream().map(SystemMessage.class::cast).toList();
  }

  @Override
  public Optional<SystemMessage> findById(Long id) {
    return dataRepository.findById(id).map(SystemMessage.class::cast);
  }

  @Override
  public SystemMessage getById(Long id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("message with id %s not found".formatted(id)));
  }

  @Override
  public void deleteById(Long id) {
    dataRepository.deleteById(id);
  }
}
