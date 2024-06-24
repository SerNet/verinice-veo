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
package org.veo.persistence.access.jpa;

import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.PERSIST;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.REMOVE;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.UPDATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.VersioningEvent;
import org.veo.persistence.entity.jpa.AbstractRiskData;
import org.veo.persistence.entity.jpa.TemplateItemData;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Collect change events for elements during a running transaction. Keeps only the most recent
 * change for each element. Previous changes will be replaced with the most recent one.
 *
 * <p>Before the transaction is committed, all pending changes will be persisted to the repository.
 */
@Slf4j
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class MostRecentChangeTracker<
    V extends Versioned & Identifiable, E extends VersioningEvent<V>> {

  private final Map<String, List<E>> allSeenChanges = new HashMap<>();

  private final Map<String, List<E>> consolidatedChanges = new HashMap<>();

  private final Set<String> idsWithPendingChanges = new HashSet<>();

  public static final String NO_ID = "no_id";

  private static final Object TRACKER_KEY = new Object();

  private final ApplicationEventPublisher publisher;

  private MostRecentChangeTracker(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  public static synchronized MostRecentChangeTracker getForCurrentTransaction(
      ApplicationEventPublisher publisher) {

    // no circus -> no monkeys
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new NoSuchElementException("No transaction is active - cannot collect change events.");
    }

    // existing tracker -> return tracker
    ChangeTrackerResourceHolder holder =
        (ChangeTrackerResourceHolder) TransactionSynchronizationManager.getResource(TRACKER_KEY);
    if (holder != null) {
      log.debug(
          "Returning existing tracker tor transaction {}",
          TransactionSynchronizationManager.getCurrentTransactionName());
      return holder.getTracker();
    }

    // no tracker -> create tracker:
    log.debug(
        "Creating new tracker tor transaction {}",
        TransactionSynchronizationManager.getCurrentTransactionName());
    var tracker = new MostRecentChangeTracker<>(publisher);
    var newHolder = new ChangeTrackerResourceHolder(tracker);
    TransactionSynchronizationManager.bindResource(TRACKER_KEY, newHolder);
    newHolder.setSynchronizedWithTransaction(true);

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.initSynchronization();
      log.warn(
          "Synchronization activated on {}. Now registering change tracker.",
          TransactionSynchronizationManager.getCurrentTransactionName());
    }
    TransactionSynchronizationManager.registerSynchronization(
        new ChangeTrackerResourceSynchronization(newHolder, TRACKER_KEY, tracker));

    return tracker;
  }

  public synchronized void put(@NonNull E eventToStore) {
    requireNonNull(eventToStore.entity());
    if (eventToStore.entity() instanceof AbstractRisk<?, ?>) {
      // only consolidate Elements. Publish risks normally.
      publisher.publishEvent(eventToStore);
      return;
    }
    log.debug(
        "Evaluate event for {} of type {} and change no. {}.",
        eventToStore.entity().getClass().getSimpleName(),
        eventToStore.type(),
        eventToStore.changeNumber());

    hydrate(eventToStore.entity());
    switch (eventToStore.type()) {
      case PERSIST -> trackEventWithoutId(eventToStore);
      case UPDATE, REMOVE -> trackEvent(eventToStore);
    }

    consolidate();
  }

  private synchronized void trackEvent(@NonNull E eventToStore) {
    if (eventToStore.type() != UPDATE && eventToStore.type() != REMOVE) {
      throw new IllegalArgumentException("Not an UPDATE or REMOVE event.");
    }
    log.debug(
        "Tracking event for {} with type {} and change no. {}",
        eventToStore.entity().getClass().getSimpleName(),
        eventToStore.type(),
        eventToStore.changeNumber());

    var id = determineId(eventToStore.entity());
    allSeenChanges.computeIfAbsent(id, k -> new ArrayList<>()).add(eventToStore);
    idsWithPendingChanges.add(id);
  }

  private synchronized void trackEventWithoutId(E eventToStore) {
    if (eventToStore.type() != PERSIST) {
      throw new IllegalArgumentException("Not a PERSIST event.");
    }
    if (determineId(eventToStore.entity()) != null) {
      var id = determineId(eventToStore.entity());
      log.debug(
          "Tracking PERSIST event with already existing ID {} for a {} with change number {}. ID should be null on newly PERSISTed entities.",
          id,
          eventToStore.entity().getClass(),
          eventToStore.changeNumber());
      allSeenChanges.computeIfAbsent(id, k -> new ArrayList<>()).add(eventToStore);
      idsWithPendingChanges.add(id);
      return;
    }

    log.debug(
        "Tracking a PERSIST event for a {} with ID null and changeNumber {}.",
        eventToStore.entity().getClass(),
        eventToStore.changeNumber());

    allSeenChanges.computeIfAbsent(NO_ID, k -> new ArrayList<>()).add(eventToStore);
    idsWithPendingChanges.add(NO_ID);
  }

  private String determineId(V entityToStore) {
    if (entityToStore instanceof AbstractRiskData<?, ?> risk) {
      return risk.getDbId();
    } else if (entityToStore instanceof TemplateItemData<?, ?> ti) {
      return ti.getDbId();
    } else {
      return entityToStore.getIdAsString();
    }
  }

  private synchronized void consolidate() {
    idsWithPendingChanges.forEach(consolidatedChanges::remove);

    // add PERSIST events:
    if (idsWithPendingChanges.contains(NO_ID)) {
      var persists = allSeenChanges.computeIfAbsent(NO_ID, k -> new ArrayList<>());
      consolidatedChanges.put(NO_ID, unmodifiableList(persists));
    }

    idsWithPendingChanges.stream()
        .filter(k -> !Objects.equals(k, NO_ID))
        .forEach(
            id -> {
              // add PERSIST events that already have an ID (edge case):
              var persistsWithId =
                  allSeenChanges.get(id).stream()
                      .filter(e -> e.type() == PERSIST && determineId(e.entity()) != null)
                      .toList();
              if (!persistsWithId.isEmpty()) {
                // get only one, order doesn't matter for inserts:
                consolidatedChanges
                    .computeIfAbsent(id, k -> new ArrayList<>())
                    .add(persistsWithId.get(0));
              }

              // add UPDATE event:
              // (add only the latest UPDATE rewritten to the lowest seen change-no of any update)
              var updates =
                  allSeenChanges.get(id).stream().filter(e -> e.type() == UPDATE).toList();
              var lowestNo = -1L;
              if (!updates.isEmpty()) {
                lowestNo = lowestSeenChangeNo(updates);
                var newestUpdate = newestEvent(updates);
                newestUpdate.entity().consolidateChangeNumber(lowestNo);
                consolidatedChanges
                    .computeIfAbsent(id, k -> new ArrayList<>())
                    .add((E) newestUpdate.withChangeNumber(lowestNo));
              }

              // add REMOVE event:
              var removes =
                  allSeenChanges.get(id).stream().filter(e -> e.type() == REMOVE).toList();
              if (!removes.isEmpty()) {
                var newestRemove = newestEvent(removes);
                if (lowestNo == -1L) {
                  // no updates tracked, use lowest change no from removes:
                  lowestNo = lowestSeenChangeNo(removes);
                } else {
                  lowestNo += 1;
                }
                newestRemove.entity().consolidateChangeNumber(lowestNo);
                consolidatedChanges
                    .computeIfAbsent(id, k -> new ArrayList<>())
                    .add((E) newestRemove.withChangeNumber(lowestNo));
              }
            });
    idsWithPendingChanges.clear();
  }

  // Reset the entity's change number to the lowest seen changeNumber.

  private long lowestSeenChangeNo(Collection<E> events) {
    return events.stream().map(VersioningEvent::changeNumber).min(Long::compare).orElseThrow();
  }

  private void hydrate(V entity) {
    if (entity instanceof Unit unit) {
      unit.getDomains().forEach(Domain::getOwner);
    } else if (entity instanceof ClientOwned owned && owned.getOwningClient() != null) {
      owned.getOwningClient().ifPresent(Client::getState);
    } else if (entity instanceof RiskAffected<?, ?> riskAffected) {
      // TODO #2841 remove this workaround when no longer needed
      riskAffected.getControlImplementations().forEach(ci -> ci.getOwner().getIdAsString());
      riskAffected.getRequirementImplementations().forEach(ri -> ri.getOrigin().getIdAsString());
    }
  }

  private E newestEvent(Collection<E> events) {
    return events.stream().max(comparingLong(VersioningEvent::changeNumber)).orElseThrow();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  @Order(HIGHEST_PRECEDENCE)
  public synchronized void publishAll() {
    log.atDebug().log(
        () ->
            "Publishing all events:%n %s"
                .formatted(
                    consolidatedChanges.values().stream()
                        .map(Object::toString)
                        .collect(joining(";\n"))));
    consolidatedChanges.values().stream().flatMap(List::stream).forEach(publisher::publishEvent);
  }

  public synchronized void clear() {
    log.debug("Clearing all events");
    this.consolidatedChanges.clear();
    this.allSeenChanges.clear();
  }
}
