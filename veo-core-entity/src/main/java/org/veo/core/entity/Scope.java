/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

/**
 * A group of {@link Element} objects that form a logical unit. An object may
 * belong to zero, one or multiple scopes. Scopes can contain scopes.
 */
public interface Scope extends Element, RiskAffected<Scope, ScopeRisk> {

    String SINGULAR_TERM = "scope";
    String PLURAL_TERM = "scopes";
    String TYPE_DESIGNATOR = "SCP";

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }

    @Override
    default Class<? extends Identifiable> getModelInterface() {
        return Scope.class;
    }

    Set<Element> getMembers();

    default boolean addMember(Element member) {
        return getMembers().add(member);
    }

    default boolean addMembers(Set<Element> members) {
        return getMembers().addAll(members);
    }

    default boolean removeMember(Element member) {
        return getMembers().remove(member);
    }

    default boolean removeMembers(Set<Element> members) {
        return getMembers().removeAll(members);
    }

    default void setMembers(Set<Element> members) {
        getMembers().clear();
        getMembers().addAll(members);
    }

    @Override
    default String getTypeDesignator() {
        return TYPE_DESIGNATOR;
    }
}
