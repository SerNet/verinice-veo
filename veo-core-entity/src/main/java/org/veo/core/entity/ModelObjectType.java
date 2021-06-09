/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;

/**
 * This class contains translations from/to all known resource collections to
 * their names & types.
 * <p>
 * This should be maintained to be the only place where new resource types must
 * be added.
 */
public enum ModelObjectType {
    // @formatter:off
    ASSET(Asset.class, Asset.PLURAL_TERM),
    CLIENT(Client.class, Client.PLURAL_TERM),
    CONTROL(Control.class, Control.PLURAL_TERM),
    CATALOG(Catalog.class, Catalog.PLURAL_TERM),
    CATALOGITEM(CatalogItem.class, CatalogItem.PLURAL_TERM),
    DOCUMENT(Document.class, Document.PLURAL_TERM),
    DOMAIN(Domain.class, Domain.PLURAL_TERM),
    DOMAINTEMPLATE(DomainTemplate.class, DomainTemplate.PLURAL_TERM),
    INCIDENT(Incident.class, Incident.PLURAL_TERM),
    PERSON(Person.class, Person.PLURAL_TERM),
    PROCESS(Process.class, Process.PLURAL_TERM),
    UNIT(Unit.class, Unit.PLURAL_TERM),
    SCENARIO(Scenario.class, Scenario.PLURAL_TERM),
    SCOPE(Scope.class, Scope.PLURAL_TERM);
    // @formatter:on

    @Getter
    private final String pluralTerm;
    @Getter
    private final Class<? extends ModelObject> type;

    ModelObjectType(Class<? extends ModelObject> type, String pluralTerm) {
        this.pluralTerm = pluralTerm;
        this.type = type;
    }

    public static Set<String> PLURAL_TERMS = Stream.of(values())
                                                   .map(et -> et.pluralTerm)
                                                   .collect(Collectors.toSet());

    public static Set<Class<? extends ModelObject>> TYPES = Stream.of(values())
                                                                  .map(et -> et.type)
                                                                  .collect(Collectors.toSet());

    public static Set<Class<? extends EntityLayerSupertype>> ENTITY_TYPES = TYPES.stream()
                                                                                 .filter(EntityLayerSupertype.class::isAssignableFrom)
                                                                                 .map(t -> (Class<? extends EntityLayerSupertype>) t)
                                                                                 .collect(Collectors.toSet());

    public static final Set<String> TYPE_DESIGNATORS = Set.of(AbstractRisk.TYPE_DESIGNATOR,
                                                              Asset.TYPE_DESIGNATOR,
                                                              Control.TYPE_DESIGNATOR,
                                                              Document.TYPE_DESIGNATOR,
                                                              Incident.TYPE_DESIGNATOR,
                                                              Person.TYPE_DESIGNATOR,
                                                              Process.TYPE_DESIGNATOR,
                                                              Scenario.TYPE_DESIGNATOR,
                                                              Scope.TYPE_DESIGNATOR);

    public static Class<? extends ModelObject> getTypeForPluralTerm(String pluralTerm) {
        return Stream.of(values())
                     .filter(et -> et.pluralTerm.equals(pluralTerm))
                     .map(et -> et.type)
                     .findFirst()
                     .orElseThrow();
    }
}
