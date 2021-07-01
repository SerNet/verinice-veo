/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate;

import java.util.Set;
import java.util.UUID;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;

public class LocalReferenceAssembler implements ReferenceAssembler {

    @Override
    public String targetReferenceOf(ModelObject modelObject) {
        return SyntheticModelObjectReference.toUrl(modelObject.getModelInterface(),
                                                   modelObject.getId()
                                                              .uuidValue());
    }

    @Override
    public String targetReferenceOf(AbstractRisk<?, ?> risk) {
        return null;
    }

    @Override
    public String searchesReferenceOf(Class<? extends ModelObject> type) {
        return null;
    }

    @Override
    public String resourcesReferenceOf(Class<? extends ModelObject> type) {
        return null;
    }

    @Override
    public Class<? extends ModelObject> parseType(String uri) {
        return null;
    }

    @Override
    public String parseId(String uri) {
        return null;
    }

    @Override
    public Key<UUID> toKey(ModelObjectReference<? extends ModelObject> reference) {
        return null;
    }

    @Override
    public Set<Key<UUID>> toKeys(Set<? extends ModelObjectReference<?>> references) {
        return null;
    }

    @Override
    public String schemaReferenceOf(String typeSingularTerm) {
        return null;
    }

}
