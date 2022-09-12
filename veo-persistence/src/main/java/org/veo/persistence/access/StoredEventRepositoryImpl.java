/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import org.veo.core.entity.event.StoredEvent;
import org.veo.persistence.access.jpa.StoredEventDataRepository;
import org.veo.persistence.entity.jpa.StoredEventData;

@Repository
public class StoredEventRepositoryImpl implements StoredEventRepository {

  private final StoredEventDataRepository dataRepository;

  public StoredEventRepositoryImpl(StoredEventDataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @Override
  public StoredEvent save(StoredEvent entity) {
    return dataRepository.save((StoredEventData) entity);
  }

  @Override
  public void saveAll(Collection<StoredEvent> events) {
    dataRepository.saveAll(events.stream().map(StoredEventData.class::cast).toList());
  }

  @Override
  public void remove(StoredEvent event) {
    dataRepository.delete((StoredEventData) event);
  }

  @Override
  public List<StoredEvent> findPendingEvents(Instant maxLockTime, int maxResults) {
    return dataRepository.findPendingEvents(maxLockTime, PageRequest.ofSize(maxResults));
  }

  @Override
  public Optional<StoredEvent> findById(Long id) {
    return dataRepository.findById(id).map(StoredEvent.class::cast);
  }

  @Override
  public void delete(StoredEvent event) {
    dataRepository.delete((StoredEventData) event);
  }
}
