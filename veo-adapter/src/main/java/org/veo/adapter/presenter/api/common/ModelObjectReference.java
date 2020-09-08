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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.ClassKey;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * A reference to another model object
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class ModelObjectReference<T extends ModelObject> implements IModelObjectReference {

    @JsonIgnore
    @ToString.Include
    @EqualsAndHashCode.Include
    private String id;

    @ToString.Include
    private String displayName;

    @JsonIgnore
    @EqualsAndHashCode.Include
    private Class<T> type;

    @JsonIgnore
    private ReferenceAssembler urlAssembler;

    private ModelObjectReference(String id, String displayName, Class<T> type,
            ReferenceAssembler referenceAssembler) {
        super();
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.urlAssembler = referenceAssembler;
    }

    private ModelObjectReference(String id, Class<T> type, ReferenceAssembler referenceAssembler) {
        super();
        this.id = id;
        this.type = type;
        this.urlAssembler = referenceAssembler;
    }

    /**
     * Create a ModelObjectReference for the given entity.
     */
    public static <T extends ModelObject> ModelObjectReference<T> from(T entity,
            @NonNull ReferenceAssembler urlAssembler) {
        Class<? extends ModelObject> modelInterface = entity.getModelInterface();
        if (modelInterface != null) {
            return new ModelObjectReference<T>(entity.getId()
                                                     .uuidValue(),
                    Switches.toDisplayNameSwitch()
                            .doSwitch(entity),
                    (Class<T>) modelInterface, urlAssembler);
        } else {
            throw new IllegalArgumentException(
                    "The given entity does not return an entity interface. " + entity.getClass()
                                                                                     .getSimpleName());
        }
    }

    public static <T extends ModelObject> ModelObjectReference<T> fromUri(String uri,
            @NonNull ReferenceAssembler urlAssembler) {

        return new ModelObjectReference<T>(urlAssembler.parseId(uri),
                (Class<T>) urlAssembler.parseType(uri), urlAssembler);
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

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getTargetUri() {
        return urlAssembler.targetReferenceOf(type, id);
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getSearchesUri() {
        return urlAssembler.searchesReferenceOf(type);
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getResourcesUri() {
        return urlAssembler.resourcesReferenceOf(type);
    }
}
