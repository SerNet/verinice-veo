/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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
package org.veo.core.entity;

import java.time.Instant;

/**
 * A versioned entity with a sequential version number and some metadata (time &
 * author of creation and last modification).
 */
public interface Versioned {
    Instant getCreatedAt();

    void setCreatedAt(Instant createdAt);

    Instant getUpdatedAt();

    void setUpdatedAt(Instant updatedAt);

    String getCreatedBy();

    void setCreatedBy(String username);

    String getUpdatedBy();

    void setUpdatedBy(String username);

    /**
     * The version number starts at 0 for a new object and is increased whenever the
     * entity is edited by the user and saved.
     */
    long getVersion();

    void setVersion(long version);

    /**
     * Copy versioning properties from another entity. This needs to be used when
     * trying to update an entity with a new instance, which should generally be
     * avoided, so this method is deprecated.
     *
     * @param username
     *            User who authors this creation / update.
     * @param storedEntity
     *            The old existing version of this entity, must not be null
     * @deprecated deprecated to encourage usage of the proper entity update
     *             strategy
     */
    @Deprecated
    default void version(String username, Versioned storedEntity) {
        var now = Instant.now();
        setUpdatedAt(now);
        setUpdatedBy(username);
        setCreatedAt(storedEntity.getCreatedAt());
        setCreatedBy(storedEntity.getCreatedBy());
        setVersion(storedEntity.getVersion());
    }
}