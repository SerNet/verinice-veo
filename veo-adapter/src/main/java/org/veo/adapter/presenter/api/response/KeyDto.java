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
package org.veo.adapter.presenter.api.response;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.core.entity.Key;

public class KeyDto {

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID can be null (for new entities) or a valid UUID string following RFC 4122. If a UUID is"
                     + "given during creation of a new entity, it will be used as ID for the generated object. "
                     + "When the same UUID is already present in the system an error will be raised instead.")
    @Schema(description = "The UUID of the entity",
            example = "f35b982c-8ad4-4515-96ee-df5fdd4247b9",
            required = false)
    private String id;

    public KeyDto(String id) {
        super();
        this.id = id;
    }

    /**
     * Map a set of KeyDTOs to a set of UUID-Keys.
     */
    public static Set<Key<UUID>> mapKeys(Set<KeyDto> keys) {
        return keys.stream()
                   .map(KeyDto::getId)
                   .map(Key::uuidFrom)
                   .collect(Collectors.toSet());
    }

    public Key<UUID> toKey() {
        return id != null ? Key.uuidFrom(id) : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        KeyDto other = (KeyDto) obj;
        return Objects.equals(id, other.id);
    }
}