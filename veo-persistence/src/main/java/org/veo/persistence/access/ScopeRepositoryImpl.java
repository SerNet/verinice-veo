/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.persistence.access;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Scope;
import org.veo.core.repository.ScopeRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ScopeRepositoryImpl extends AbstractEntityLayerSupertypeRepository<Scope, ScopeData>
        implements ScopeRepository {

    public ScopeRepositoryImpl(ScopeDataRepository dataRepository, ValidationService validation,
            CustomLinkDataRepository linkDataRepository, ScopeDataRepository scopeDataRepository) {
        super(dataRepository, validation, linkDataRepository, scopeDataRepository);
    }

}