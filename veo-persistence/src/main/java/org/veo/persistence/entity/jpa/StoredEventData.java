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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.veo.core.entity.event.StoredEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class StoredEventData implements StoredEvent {

    /**
     * This uses a sequence with a large increment to make ID generation more
     * efficient. This can lead to gaps in the IDs if the application is restarted,
     * so the consumer must not rely on IDs being consecutive.
     *
     * @see <a href=
     *      "https://vladmihalcea.com/hibernate-hidden-gem-the-pooled-lo-optimizer">https://vladmihalcea.com/hibernate-hidden-gem-the-pooled-lo-optimizer</a>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_events")
    @SequenceGenerator(name = "seq_events", allocationSize = 50)
    private Long id;

    @Column(length = 100000)
    private String content;
    private String routingKey;
    @Column(nullable = false)
    private Boolean processed = false;
    private Instant lockTime;

    private Instant timestamp;

    @Override
    public boolean markAsProcessed() {
        if (processed)
            return false;
        processed = true;
        return true;
    }

    @Override
    public void lock() {
        lockTime = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof StoredEventData))
            return false;

        StoredEventData other = (StoredEventData) o;
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
        return new StoredEventData(null, content, routingKey, false, null, Instant.now());
    }
}
