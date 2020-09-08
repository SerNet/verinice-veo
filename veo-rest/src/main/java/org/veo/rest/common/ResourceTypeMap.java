/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.rest.common;

import java.util.Map;
import java.util.Set;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;

/**
 * This class contains translations from/to all known resource collections to
 * their names/URIs.
 * <p>
 * This should be maintained to be the only place where new resource types must
 * be added.
 */
public final class ResourceTypeMap {

    public static final String PERSONS = "persons";
    public static final String PROCESSES = "processes";
    public static final String CONTROLS = "controls";
    public static final String ASSETS = "assets";
    public static final String UNITS = "units";
    public static final String DOMAINS = "domains";
    public static final String GROUPS = "groups";

    /**
     * Groups are mapped to their member's entity type. They are therefor not
     * included in this list or any of the mapping methods.
     */
    public static final Set<String> KNOWN_COLLECTION_NAMES = Set.of(PERSONS, PROCESSES, CONTROLS,
                                                                    ASSETS, UNITS, DOMAINS);
    private static final Map<Class, String> typeToCollection = Map.of(Person.class, PERSONS,
                                                                      Process.class, PROCESSES,
                                                                      Control.class, CONTROLS,
                                                                      Asset.class, ASSETS,
                                                                      Unit.class, UNITS,
                                                                      Domain.class, DOMAINS);

    private static final Map<String, Class> collectionToType = Map.of(PERSONS, Person.class,
                                                                      PROCESSES, Process.class,
                                                                      CONTROLS, Control.class,
                                                                      ASSETS, Asset.class, UNITS,
                                                                      Unit.class, DOMAINS,
                                                                      Domain.class);

    public static String getCollectionNameFor(Class type) {
        return typeToCollection.get(type);
    }

    public static Class getTypeForCollectionName(String name) {
        return collectionToType.get(name);
    }

    public static Set<String> getKnownCollectionNames() {
        return KNOWN_COLLECTION_NAMES;
    }
}
