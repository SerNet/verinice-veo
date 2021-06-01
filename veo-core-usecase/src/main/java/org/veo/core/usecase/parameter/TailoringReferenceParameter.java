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
package org.veo.core.usecase.parameter;

import org.veo.core.entity.Catalogable;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@AllArgsConstructor
/**
 * The {@link TailoringReferenceParameter} describes the concrete values to
 * apply a defined {@link TailoringReference}.
 */
public class TailoringReferenceParameter extends CatalogReferenceParameter
        implements TailoringReferenceTyped {
    @EqualsAndHashCode.Include
    private TailoringReferenceType referenceType;
    /**
     * The translatable Key of the described reference. Currently only CustomLinks
     * are supported and in this case the referenceKey is the type of the
     * customLink.
     */
    @EqualsAndHashCode.Include
    private String referenceKey;

    public TailoringReferenceParameter(Catalogable linkedCatalogable,
            TailoringReferenceType referenceType, String referenceName) {
        this(referenceType, referenceName);
        setReferencedCatalogable(linkedCatalogable);
    }
}
