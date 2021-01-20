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
package org.veo.core.entity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains translations from/to all known resource collections to
 * their names/URIs.
 * <p>
 * This should be maintained to be the only place where new resource types must
 * be added.
 */
public final class EntityTypeNames {

    public static final String ASSET = "asset";
    public static final String CLIENT = "client";
    public static final String CONTROL = "control";
    public static final String DOCUMENT = "document";
    public static final String DOMAIN = "domain";
    public static final String INCIDENT = "incident";
    public static final String PERSON = "person";
    public static final String PROCESS = "process";
    public static final String UNIT = "unit";
    public static final String SCENARIO = "scenario";
    public static final String SCOPE = "scope";

    public static final String ASSETS = "assets";
    public static final String CLIENTS = "clients";
    public static final String CONTROLS = "controls";
    public static final String DOCUMENTS = "documents";
    public static final String DOMAINS = "domains";
    public static final String INCIDENTS = "incidents";
    public static final String SCOPES = "scopes";
    public static final String PERSONS = "persons";
    public static final String PROCESSES = "processes";
    public static final String UNITS = "units";
    public static final String SCENARIOS = "scenarios";

    public static final Set<String> KNOWN_COLLECTION_NAMES = Set.of(PERSONS, PROCESSES, CONTROLS,
                                                                    DOCUMENTS, ASSETS, UNITS,
                                                                    SCENARIOS, INCIDENTS, SCOPES,
                                                                    DOMAINS);
    private static final Map<Class<? extends ModelObject>, String> typeToCollection = Map.ofEntries(Map.entry(Person.class,
                                                                                                              PERSONS),
                                                                                                    Map.entry(Process.class,
                                                                                                              PROCESSES),
                                                                                                    Map.entry(Control.class,
                                                                                                              CONTROLS),
                                                                                                    Map.entry(Document.class,
                                                                                                              DOCUMENTS),
                                                                                                    Map.entry(Incident.class,
                                                                                                              INCIDENTS),
                                                                                                    Map.entry(Scenario.class,
                                                                                                              SCENARIOS),
                                                                                                    Map.entry(Asset.class,
                                                                                                              ASSETS),
                                                                                                    Map.entry(Scope.class,
                                                                                                              SCOPES),
                                                                                                    Map.entry(Client.class,
                                                                                                              CLIENTS),
                                                                                                    Map.entry(Unit.class,
                                                                                                              UNITS),
                                                                                                    Map.entry(Domain.class,
                                                                                                              DOMAINS));

    private static final Map<String, Class<? extends ModelObject>> collectionToType = typeToCollection.entrySet()
                                                                                                      .stream()
                                                                                                      .collect(Collectors.toMap(Map.Entry::getValue,
                                                                                                                                Map.Entry::getKey));
    public static final Set<Class<? extends EntityLayerSupertype>> ENTITY_CLASS_LIST = Set.of(Asset.class,
                                                                                              Control.class,
                                                                                              Document.class,
                                                                                              Person.class,
                                                                                              Incident.class,
                                                                                              Scenario.class,
                                                                                              Process.class,
                                                                                              Scope.class);

    public static String getCollectionNameFor(Class<? extends ModelObject> type) {
        return typeToCollection.get(type);
    }

    public static Class<? extends ModelObject> getTypeForCollectionName(String name) {
        return collectionToType.get(name);
    }

    public static Set<String> getKnownCollectionNames() {
        return KNOWN_COLLECTION_NAMES;
    }

    public static Set<Class<? extends EntityLayerSupertype>> getKnownEntityClasses() {
        return ENTITY_CLASS_LIST;
    }

    private EntityTypeNames() {
    }
}
