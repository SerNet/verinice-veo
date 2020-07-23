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
package org.veo.adapter.presenter.api.response.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.transform.ClassKey;
import org.veo.core.entity.transform.TransformContext;

public class DtoToEntityContext implements TransformContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static DtoToEntityContext getCompleteTransformationContext() {
        return new DtoToEntityContext();
    }

    private Map<ClassKey<Key<UUID>>, ? super ModelObject> context = new HashMap<>();

    public Map<ClassKey<Key<UUID>>, ? super ModelObject> getContext() {
        return context;
    }

    /**
     * Add an entity to the context which will be used in the transformation for
     * matching object. An object need the corresponding Dto type and the same id to
     * Match.
     */
    public void addEntity(ModelObject entity) {
        Class<?>[] interfaces = Stream.of(entity.getClass()
                                                .getInterfaces())
                                      .filter(ModelObject.class::isAssignableFrom)
                                      .filter(Predicate.isEqual(ModelGroup.class)
                                                       .negate())
                                      .toArray(Class[]::new);
        if (interfaces.length == 1) {
            ClassKey<Key<UUID>> classKey = new ClassKey<>(interfaces[0], entity.getId());
            context.put(classKey, entity);
        } else {
            throw new IllegalArgumentException(
                    "The given entity implements more than one interface.");
        }
    }
}
