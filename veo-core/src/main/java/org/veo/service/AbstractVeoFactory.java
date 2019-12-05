/*******************************************************************************
 * Copyright (c) 2018 Urs Zeidler.
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
package org.veo.service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the basic class for the object factories like {@link LinkFactory} or
 * {@link ElementFactory}.
 */
public class AbstractVeoFactory {

    private static final Set<String> STATIC_PROPERTIES;

    static {
        STATIC_PROPERTIES = Stream.of(JsonFactory.ID, JsonFactory.PARENT, JsonFactory.TITLE,
                                      JsonFactory.TYPE)
                                  .collect(Collectors.toSet());
    }

    protected AbstractVeoFactory() {
    }

    protected boolean isStaticProperty(String name) {
        return AbstractVeoFactory.STATIC_PROPERTIES.contains(name);
    }

}