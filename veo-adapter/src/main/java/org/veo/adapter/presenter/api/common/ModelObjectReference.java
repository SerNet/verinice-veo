/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.ClassKey;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A reference to another model object
 */
@Data
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ModelObjectReference<T extends ModelObject> implements IModelObjectReference {

    @JsonIgnore
    @ToString.Include
    private String id;
    @ToString.Include
    private String displayName;
    @JsonIgnore
    private Class<T> type;
    @JsonIgnore
    private String basePath = "";

    public ModelObjectReference(String href) {
        this(StringUtils.EMPTY, href);
    }

    public ModelObjectReference(String displayName, String href) {
        this.displayName = displayName;
        type = (Class<T>) ModelObject.class;
        setHref(href);
    }

    public ModelObjectReference(String id, String displayName, Class<T> type) {
        super();
        this.id = id;
        this.displayName = displayName;
        this.type = type;
    }

    public String getHref() {
        return basePath + "/" + type.getSimpleName()
                                    .toLowerCase()
                + "s/" + id;
    }

    @SuppressWarnings("unchecked")
    public final void setHref(String href) {
        // FIXME VEO-118 This code needs love
        if (href != null) {
            String[] parts = href.split("/");
            id = parts[2];
            String simpleName = parts[1].substring(0, 1)
                                        .toUpperCase()
                    + parts[1].substring(1, parts[1].length() - 1); // FIXME VEO-118 removes the 's"
                                                                    // - needs
                                                                    // to be done by controller
            String className = "org.veo.core.entity." + simpleName;
            try {
                type = (Class<T>) Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new NotFoundException("", parts[0], parts[1], parts[2]);
            }
        }
    }

    /**
     * Uses the resource URL from a received reference (i.e. during a PUT request)
     * to create a reference object to the correct type.
     *
     * @param href
     * @return
     */
    public static ModelObjectReference<?> fromReference(String href, String baseURL) {
        // TODO cut the baseURL and parse the following of the href and split into type
        // and id.
        // Must strictly match the REST endpoint defined in the controller for security
        // reasons.
        return null;
    }

    /**
     * Maps the given reference to an entity in the context map.
     *
     * @param <T>
     *            the type to map
     * @param context
     *            the transformcontext
     * @param ref
     *            the modelref of type T
     * @return the mapped entity of type T
     * @throws RuntimeException
     *             when no entity is found
     */
    public static <T extends ModelObject> T mapToEntity(
            Map<ClassKey<Key<UUID>>, ? super ModelObject> context, ModelObjectReference<T> ref) {
        ClassKey<Key<UUID>> key = new ClassKey<>(ref.getType(), Key.uuidFrom(ref.getId()));
        @SuppressWarnings("unchecked")
        T object = (T) context.get(key);
        if (object == null)
            throw new NotFoundException("No object found for type:" + ref.getType()
                                                                         .getSimpleName()
                    + " and id:" + ref.getId());
        return object;
    }

    /**
     * Create a ModelObjectReference for the given entity.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ModelObject> ModelObjectReference<T> from(T entity) {
        Class<? extends ModelObject> modelInterface = entity.getModelInterface();
        if (modelInterface != null) {
            ModelObjectReference<T> modelObjectReference = new ModelObjectReference<>(entity.getId()
                                                                                            .uuidValue(),
                    Switches.toDisplayNameSwitch()
                            .doSwitch(entity),
                    (Class<T>) modelInterface);
            modelObjectReference.setId(entity.getId()
                                             .uuidValue());
            return modelObjectReference;
        } else {
            throw new IllegalArgumentException(
                    "The given entity does not return an entity interface. " + entity.getClass()
                                                                                     .getSimpleName());
        }
    }

}
