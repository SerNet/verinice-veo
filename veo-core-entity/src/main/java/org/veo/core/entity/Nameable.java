/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

/**
 * Defines the basic properties of nameable elements.
 */
public interface Nameable extends Displayable {

    String getName();

    void setName(String aName);

    String getAbbreviation();

    void setAbbreviation(String aAbbreviation);

    String getDescription();

    void setDescription(String aDescription);

    /**
     * A default implementation to render a user friendly display name that can be
     * overridden if more complicated parameters need to be taken into account.
     * <p>
     * I.e. for a person the title, middle initial et al might have to be included
     * in the display name, the nationality of the person and the UI locale might
     * need to be considered etc.
     * <p>
     * This is why more complicated implementations of this method should be placed
     * in the adapter layer.
     * <p>
     * See also:
     * <p>
     * {@code org.veo.adapter.presenter.api.common.ToDisplayNameSwitch}
     */
    default String getDisplayName() {
        return String.join("",
                           (getAbbreviation() != null && !getAbbreviation().isEmpty())
                                   ? getAbbreviation() + " "
                                   : "",
                           getName());
    }

}
