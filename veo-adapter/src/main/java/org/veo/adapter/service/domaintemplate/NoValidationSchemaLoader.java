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

import org.veo.adapter.presenter.api.response.transformer.EntitySchema;
import org.veo.adapter.presenter.api.response.transformer.EntitySchemaLoader;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;

/**
 * A json schema loader which loads nothing and always validates.
 */
public final class NoValidationSchemaLoader extends EntitySchemaLoader {
    static final NoValidationEntitySchema NO_VALIDATION = new NoValidationEntitySchema();
    public static final NoValidationSchemaLoader NO_VALIDATION_LOADER = new NoValidationSchemaLoader();

    private static final class NoValidationEntitySchema extends EntitySchema {
        private NoValidationEntitySchema() {
            super(null);
        }

        @Override
        public void validateCustomAspect(CustomAspect customAspect) {
        }

        @Override
        public void validateCustomLink(CustomLink customLink) {
        }
    }

    private NoValidationSchemaLoader() {
        super(null);
    }

    @Override
    public EntitySchema load(String entityType) {
        return NO_VALIDATION;
    }
}