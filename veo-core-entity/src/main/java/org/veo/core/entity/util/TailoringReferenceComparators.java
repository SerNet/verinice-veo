/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.core.entity.util;

import java.util.Comparator;

import org.veo.core.entity.TailoringReference;

public final class TailoringReferenceComparators {

    private TailoringReferenceComparators() {
    }

    public static final Comparator<? super TailoringReference> BY_CATALOGITEM_ELEMENT = (c1,
            c2) -> c1.getCatalogItem()
                     .getElement()
                     .getId()
                     .uuidValue()
                     .compareTo(c2.getCatalogItem()
                                  .getElement()
                                  .getId()
                                  .uuidValue());
    /**
     * Orders {@link TailoringReference}s for application. They are sorted first
     * alphabetically by their reference type and then by their catalog-item's
     * element UUID string value.
     */
    public static final Comparator<? super TailoringReference> BY_EXECUTION = Comparator.comparing(TailoringReference::getReferenceType)
                                                                                        .thenComparing(BY_CATALOGITEM_ELEMENT);

}
