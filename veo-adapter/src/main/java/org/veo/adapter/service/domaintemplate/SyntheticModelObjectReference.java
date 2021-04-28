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

import java.lang.reflect.Field;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.ModelObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyntheticModelObjectReference<T extends ModelObject> extends ModelObjectReference<T> {
    public SyntheticModelObjectReference(String id, Class<T> type,
            ReferenceAssembler urlAssembler) {
        super(id, null, type, null, null, null);
    }

    @Override
    public String getSearchesUri() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getResourcesUri() {
        return null;
    }

    @Override
    public String getTargetUri() {
        return toUrl(getType(), getId());
    }

    public static <T extends ModelObject> SyntheticModelObjectReference<T> from(String id,
            Class<T> type) {
        return new SyntheticModelObjectReference<>(id, type, null);
    }

    public static <T extends ModelObject> SyntheticModelObjectReference<T> from(String id,
            Class<T> declaredType, Class<?> realType) {
        return new SyntheticModelObjectReference<T>(id, (Class<T>) realType, null);
    }

    public static <T extends ModelObject> String toUrl(Class<T> type, String id) {
        try {
            return "/" + toPluralTerm(type) + "/" + id;
        } catch (SecurityException | NoSuchFieldException | IllegalAccessException e) {
            log.error("Error generating target uri", e);
        }
        return null;
    }

    public static <T extends ModelObject> String toPluralTerm(Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = type.getField("PLURAL_TERM");
        Object object = field.get(type);
        return (String) object;
    }

    public static <T extends ModelObject> String toSingularTerm(Class<T> type) {
        try {
            Field field = type.getField("SINGULAR_TERM");
            Object object = field.get(type);
            return (String) object;
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                | IllegalAccessException e) {
            log.error("Error toSingularTerm", e);
        }
        return null;
    }
}