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
package org.veo.core.entity.format;

import java.util.List;
import java.util.Objects;

import lombok.NonNull;

/**
 * A display name format for a certain type of source object. Supports
 * placeholders that are resolved on a source object so the resolved values can
 * be rendered into the display name.
 */
public class DisplayNameFormat<TSource> {
    private final List<Placeholder<TSource>> placeholders;
    private final String format;

    /**
     * @param format
     *            Format string in the {@link String#format} style (e.g. "%s - %s
     *            (%s)")
     * @param placeholders
     *            * List of placeholders to be resolved.
     */
    public DisplayNameFormat(String format, List<Placeholder<TSource>> placeholders) {
        this.format = format;
        this.placeholders = placeholders;
    }

    /**
     * Renders a display name for given source.
     *
     * @param source
     *            Source object to resolve placeholders with.
     * @return Formatted string
     */
    public String render(@NonNull TSource source) {
        var values = placeholders.stream()
                                 .map(placeholder -> placeholder.resolveValue(source))
                                 .map(value -> Objects.requireNonNullElse(value, ""))
                                 .toArray();
        return String.format(format, values)
                     .trim();
    }
}
