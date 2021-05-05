/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.entity.specification;

import java.util.UUID;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;

/**
 * Utility class to create entity specifications
 */
public final class EntitySpecifications {

    private static final EntitySpecification<ModelObject> MATCH_ALL = modelObject -> true;

    public static <T extends ModelObject> EntitySpecification<T> matchAll() {
        return (EntitySpecification<T>) MATCH_ALL;
    }

    public static EntitySpecification<Domain> isActive() {
        return Domain::isActive;
    }

    public static EntitySpecification<ModelObject> hasId(Key<UUID> id) {
        return o -> o.getId()
                     .equals(id);
    }

    public static EntitySpecification<CatalogItem> hasNamespace(String namespace) {
        return catalogItem -> namespace.equals(catalogItem.getNamespace());
    }

    public static <T extends EntityLayerSupertype> SameClientSpecification<T> hasSameClient(
            Client client) {
        return new SameClientSpecification<>(client);
    }

    public static ValidUnitSpecification<EntityLayerSupertype> hasValidUnit() {
        return ValidUnitSpecification.INSTANCE;
    }

    private EntitySpecifications() {

    }

}
