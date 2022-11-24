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
package org.veo.persistence.entity.jpa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.veo.core.entity.event.StoredEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class StoredEventData implements StoredEvent {

  /**
   * This uses a sequence with a large increment to make ID generation more efficient. This can lead
   * to gaps in the IDs if the application is restarted, so the consumer must not rely on IDs being
   * consecutive.
   *
   * @see <a href=
   *     "https://vladmihalcea.com/hibernate-hidden-gem-the-pooled-lo-optimizer">https://vladmihalcea.com/hibernate-hidden-gem-the-pooled-lo-optimizer</a>
   */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_events")
  @SequenceGenerator(name = "seq_events", allocationSize = 50)
  private Long id;

  @Column(length = 10000000)
  private String content;

  private String routingKey;

  private Instant lockTime;

  private Instant timestamp;

  @Override
  public void lock() {
    var newLockTime = Instant.now();
    log.debug("Setting lockTime on event {} to: {}", id, newLockTime);
    if (lockTime != null) {
      log.warn("Event {} was previously locked at {}. Relocking to {}", id, lockTime, newLockTime);
    }
    lockTime = newLockTime;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof StoredEventData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    Long dbId = getId();
    return dbId != null && dbId.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  public static StoredEventData newInstance(String content, String routingKey) {
    return new StoredEventData(null, content, routingKey, null, Instant.now());
  }
}
