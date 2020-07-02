/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.persistence.entity.jpa.transformer;

import java.util.*;
import java.util.stream.Collectors;

import org.veo.core.entity.CustomProperties;
import org.veo.persistence.entity.jpa.custom.*;

class PropertyDataMapper {
    public static Set<PropertyData> getPropertyDataSet(CustomProperties source) {
        var set = new HashSet<PropertyData>();
        addPropertyData(set, source.getBooleanProperties(), PropertyData::new);
        addPropertyData(set, source.getIntegerProperties(), PropertyData::new);
        addPropertyData(set, source.getOffsetDateTimeProperties(), PropertyData::new);
        addPropertyData(set, source.getStringProperties(), PropertyData::new);
        addPropertyData(set, source.getStringListProperties(), PropertyData::new);
        return set;
    }

    private static <T> void addPropertyData(Set<PropertyData> target, Map<String, T> input,
            PropertyDataSupplier<T> supplier) {
        target.addAll(input.entrySet()
                           .stream()
                           .map(e -> supplier.create(e.getKey(), e.getValue()))
                           .collect(Collectors.toList()));
    }

    interface PropertyDataSupplier<T> {
        PropertyData create(String key, T value);
    }
}
