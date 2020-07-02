/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.response.transformer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.veo.core.entity.CustomProperties;

class PropertyTransformer {

    public void applyDtoPropertiesToEntity(Map<String, ?> source, CustomProperties target) {
        source.forEach((key, value) -> {
            if (value instanceof Boolean) {
                target.setProperty(key, (Boolean) value);
            } else if (value instanceof Integer) {
                target.setProperty(key, (Integer) value);
            } else if (value instanceof String) {
                target.setProperty(key, (String) value);
            } else if (value instanceof List) {
                for (var item : (List<?>) value) {
                    if (!(item instanceof String)) {
                        throw new IllegalArgumentException(
                                "Illegal custom property list value type. Expected String, "
                                        + "got  " + item.getClass());
                    }
                }
                target.setProperty(key, (List<String>) value);
            }
            // TODO: Handle date times correctly (actual maps from the client have time
            // values as strings).
            else if (value instanceof OffsetDateTime) {
                target.setProperty(key, (OffsetDateTime) value);
            } else {
                throw new IllegalArgumentException(
                        "Illegal custom property value type: " + value.getClass());
            }
        });
    }
}
