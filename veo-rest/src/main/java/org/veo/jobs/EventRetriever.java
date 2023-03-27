/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.jobs;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.event.StoredEvent;
import org.veo.persistence.access.StoredEventRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Wraps event retrieval and locking in a dedicated transaction. See the remarks on the EventSender
 * class for a more detailed explanation.
 *
 * <p>The event retrieval needs to run in its own read-write transaction because the retrieved
 * events are time-locked to prevent another MessagingJob from working on the same retrieved events.
 * This needs to be an atomic transaction that is committed before this MessagingJob begins working
 * on the retrieved events.
 */
@Service
@Slf4j
public class EventRetriever {

  private final StoredEventRepository storedEventRepository;

  public EventRetriever(StoredEventRepository storedEventRepository) {
    this.storedEventRepository = storedEventRepository;
  }

  /**
   * Retrieve and lock retrieved events. {@code Serializable} isolation level prevents phantom
   * reads.
   */
  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Retryable(
      value = {
        ObjectOptimisticLockingFailureException.class,
        PessimisticLockingFailureException.class,
        TransactionSystemException.class,
        UncategorizedSQLException.class
      },
      maxAttempts = 5,
      backoff = @Backoff(delay = 250, maxDelay = 1000, multiplier = 2))
  public List<StoredEvent> retrievePendingEvents(Duration lockExpiration, int chunkSize) {
    var now = Instant.now();
    var maxLockTime = now.minus(lockExpiration);
    var events = storedEventRepository.findPendingEvents(maxLockTime, chunkSize);
    if (events.isEmpty()) {
      log.debug("Retrieved no events");
      return Collections.emptyList();
    }

    log.debug("maxLockTime is: {}", maxLockTime);
    log.debug("Retrieved {} events.", events.size());
    log.debug("lockExpiration is: {}", lockExpiration);
    log.debug("Now is: {}", now);

    events.forEach(StoredEvent::lock);
    storedEventRepository.saveAll(events);
    return events;
  }

  @Recover
  public List<StoredEvent> recover(ObjectOptimisticLockingFailureException e) {
    log.error("Could not retrieve messages, max number of retries has been reached.", e);
    throw e;
  }

  @Recover
  public List<StoredEvent> recover(PessimisticLockingFailureException e) {
    log.error("Could not retrieve messages, max number of retries has been reached.", e);
    throw e;
  }

  @Recover
  public List<StoredEvent> recover(TransactionSystemException e) {
    log.error("Could not retrieve messages, max number of retries has been reached.", e);
    throw e;
  }

  @Recover
  public List<StoredEvent> recover(UncategorizedSQLException e) {
    // see https://github.com/spring-projects/spring-boot/pull/25493
    log.error("Could not retrieve messages, max number of retries has been reached.", e);
    throw e;
  }
}
