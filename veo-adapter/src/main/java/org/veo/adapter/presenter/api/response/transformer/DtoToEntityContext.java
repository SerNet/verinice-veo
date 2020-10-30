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

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.TransformContext;
import org.veo.core.service.EntitySchemaService;

public class DtoToEntityContext implements TransformContext {

    public DtoToEntityContext(EntityFactory entityFactory, EntitySchemaService entitySchemaService,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        this.factory = entityFactory;
        this.entitySchemaLoader = new EntitySchemaLoader(entitySchemaService);
        this.modelObjectReferenceResolver = modelObjectReferenceResolver;
    }

    private final EntityFactory factory;
    private final EntitySchemaLoader entitySchemaLoader;
    private final ModelObjectReferenceResolver modelObjectReferenceResolver;

    public EntityFactory getFactory() {
        return factory;
    }

    public EntitySchema loadEntitySchema(String entityType) {
        return entitySchemaLoader.load(entityType);
    }

    public <T extends ModelObject> T resolve(ModelObjectReference<T> reference) {
        return modelObjectReferenceResolver.resolve(reference);
    }
}
