/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.Data;

import org.veo.core.entity.Key;

@Data

@Embeddable
public class SimpleKey implements Serializable {

    @Column(name = "uuid")
    private final String uuid;

    public static SimpleKey from(Key<UUID> key) {
        return new SimpleKey(key.uuidValue());
    }

    public Key<UUID> toKey() {
        return Key.uuidFrom(this.uuid);
    }

    public static Set<SimpleKey> from(Set<Key<UUID>> ids) {
        return ids.stream()
                  .map(SimpleKey::from)
                  .collect(Collectors.toSet());

    }

}
