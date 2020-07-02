/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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

import java.util.Set;

/**
 * The abstract base model class. Used to prevent duplicating common methods in
 * model layer objects.
 */
public interface EntityLayerSupertype extends NameAble, ModelObject {

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    boolean addToDomains(Domain aDomain);

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    boolean removeFromDomains(Domain aDomain);

    Set<Domain> getDomains();

    void setDomains(Set<Domain> aDomains);

    /**
     * Add the given CustomLink to the collection links. Adding will set the source
     * to this.
     *
     * @return true if added
     */
    boolean addToLinks(CustomLink aCustomLink);

    /**
     * Remove the given CustomLink from the collection links. Removing will set the
     * source to null.
     *
     * @return true if removed
     */
    boolean removeFromLinks(CustomLink aCustomLink);

    Set<CustomLink> getLinks();

    void setLinks(Set<CustomLink> aLinks);

    /**
     * Add the given CustomProperties to the collection customAspects.
     *
     * @return true if added
     */
    boolean addToCustomAspects(CustomProperties aCustomProperties);

    /**
     * Remove the given CustomProperties from the collection customAspects.
     *
     * @return true if removed
     */
    boolean removeFromCustomAspects(CustomProperties aCustomProperties);

    Set<CustomProperties> getCustomAspects();

    void setCustomAspects(Set<CustomProperties> aCustomAspects);

    Unit getOwner();

    void setOwner(Unit aOwner);

}
