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
package org.veo.persistence.entity.jpa;

import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author urszeidler
 */
@MappedSuperclass
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@RequiredArgsConstructor
@EntityListeners(ModelObjectDataEntityListener.class)
public abstract class BaseModelObjectData extends VersionedData implements ModelObject {

    @Override
    public Key<UUID> getId() {
        return Key.uuidFrom(getDbId());
    }

    @Override
    public void setId(Key<UUID> id) {
        setDbId(Optional.ofNullable(id)
                        .map(Key::uuidValue)
                        .orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof BaseModelObjectData))
            return false;

        BaseModelObjectData other = (BaseModelObjectData) o;
        // Transient (unmanaged) entities have an ID of 'null'. Only managed
        // (persisted and detached) entities have an identity. JPA requires that
        // an entity's identity remains the same over all state changes.
        // Therefore a transient entity must never equal another entity.
        String dbId = getDbId();
        return dbId != null && dbId.equals(other.getDbId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
