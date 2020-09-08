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
package org.veo.adapter.presenter.api.common;

import org.veo.core.entity.ModelObject;

/**
 * Construct and deconstruct references to model objects.
 *
 * These may be HTTP-URLs in a REST application with the correct hostname based
 * on the received request.
 */
public interface ReferenceAssembler {

    /**
     * Returns an absolute reference to the target object of this reference.
     *
     * @param type
     *            the model object type
     * @param id
     *            the ID of the target object
     * @return the URI of the specific target object
     */
    String targetReferenceOf(Class<? extends ModelObject> type, String id);

    /**
     * Returns a reference to a collection of searches for the target type. Searches
     * are a ressource so they can be created, requested and deleted (think: "named
     * search").
     *
     * @param type
     *            the model object type
     * @return the URI of the resource collection for searches or {@code null} if
     *         the resource does not support searches.
     */
    String searchesReferenceOf(Class<? extends ModelObject> type);

    /**
     * Returns a reference to the resource collection of the target type.
     *
     * @param type
     *            the type of model object
     * @return the URI of the resource collection or {@code null} if no resource
     *         collection for the type is exposed.
     */
    String resourcesReferenceOf(Class<? extends ModelObject> type);

    /**
     * Extract the objects type from the given URI.
     *
     * @param uri
     *            the URI which may be a URL.
     * @returnThe class of the correct type of model object.
     */
    Class<? extends ModelObject> parseType(String uri);

    /**
     * Extract the objects ID from the given URI.
     *
     * @param uri
     *            the URI which may be a URL
     *
     * @return
     */
    String parseId(String uri);
}
