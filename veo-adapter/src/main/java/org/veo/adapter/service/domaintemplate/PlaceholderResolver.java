/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.adapter.DbIdRefResolver;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.exception.NotFoundException;

class PlaceholderResolver extends DbIdRefResolver {
    Map<String, Identifiable> cache = new HashMap<>();
    Map<String, IdentifiableDto> dtoCache = new HashMap<>();

    private final DtoToEntityTransformer entityTransformer;

    PlaceholderResolver(DtoToEntityTransformer entityTransformer) {
        super(null, null);
        this.entityTransformer = entityTransformer;
    }

    @Override
    public <TEntity extends Identifiable> TEntity resolve(IdRef<TEntity> objectReference)
            throws NotFoundException {
        if (objectReference == null) {
            return null;
        }
        String id = objectReference.getId();
        Identifiable identifiable = cache.computeIfAbsent(id,
                                                          a -> createElement(id,
                                                                             objectReference.getType()));
        return (TEntity) identifiable;
    }

    @Override
    public <TEntity extends Identifiable> Set<TEntity> resolve(
            Set<IdRef<TEntity>> objectReferences) {

        return objectReferences.stream()
                               .map(o -> resolve(o))
                               .collect(Collectors.toSet());
    }

    /**
     * Creates the missing element from the dto in the cache.
     */
    private Identifiable createElement(String id, Class<? extends Identifiable> type) {
        IdentifiableDto elementDto = dtoCache.get(id);
        if (elementDto != null) {
            AbstractElementDto es = (AbstractElementDto) elementDto;
            HashMap<String, List<CustomLinkDto>> hashMap = new HashMap<>(es.getLinks());
            es.getLinks()
              .clear();
            Element element = entityTransformer.transformDto2Element(es, this);
            es.getLinks()
              .putAll(hashMap);
            return element;
        }
        throw new IllegalArgumentException("Unknown type (not dtoCached):" + type + "  id:" + id);
    }

}