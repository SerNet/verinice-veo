/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.core.entity.util;

import static java.util.Optional.ofNullable;

import java.util.Comparator;

import org.veo.core.entity.CustomLink;

public final class CustomLinkComparators {

    private CustomLinkComparators() {
    }

    /**
     * Orders a string alphabetically. {@code null} values are treated as empty
     * strings.
     */
    public static final Comparator<? super String> BY_STRING_NULL_SAFE = (s1,
            s2) -> ofNullable(s1).orElse("")
                                 .compareTo(ofNullable(s2).orElse(""));

    public static final Comparator<? super CustomLink> BY_LINK_TARGET = (c1, c2) -> c1.getTarget()
                                                                                      .getId()
                                                                                      .uuidValue()
                                                                                      .compareTo(c2.getTarget()
                                                                                                   .getId()
                                                                                                   .uuidValue());
    /**
     * Orders the links for application: first alphabetically by their type name,
     * then alphabetically by their target element's UUID string value.
     */
    public static final Comparator<? super CustomLink> BY_LINK_EXECUTION = Comparator.comparing(CustomLink::getType,
                                                                                                CustomLinkComparators.BY_STRING_NULL_SAFE)
                                                                                     .thenComparing(BY_LINK_TARGET);

}
