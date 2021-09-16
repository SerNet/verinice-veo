/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nonnull;

import com.diffplug.spotless.FormatterStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Custom JSON formatter using Jackson to sort object keys alphabetically.
 */
public final class JsonSortStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String NAME = "jsonSort";
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    /**
     * Creates a FormatterStep which forbids wildcard imports.
     */
    public static FormatterStep create() {
        return FormatterStep.create(JsonSortStep.NAME, new JsonSortStep(), step -> step::format);
    }

    /** Formats the given string. */
    public @Nonnull String format(String raw) throws JsonProcessingException {
        return objectMapper.writeValueAsString(objectMapper.readValue(raw, Map.class));
    }
}