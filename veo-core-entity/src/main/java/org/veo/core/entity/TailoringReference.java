/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
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
package org.veo.core.entity;

/**
 * TailoringReference Refers another catalog item in this catalog which are
 * connected and need to be applied also. Like a set of controls connected to a
 * scenario. The following constrains applies to the tailoring refs: 1. The
 * reference catalogItem always point to a catalogitem in the same catalog. 2.1.
 * All references defined by the catalogable, need to refers a Catalogable of a
 * catalogitem in the same catalog. 2.2. For each such reference a coresponding
 * tailref of type LINK must exist, pointing to the catalogItem which holds the
 * refered element.
 */
public interface TailoringReference extends CatalogReference {
    String SINGULAR_TERM = "tailoringreference";
    String PLURAL_TERM = "tailoringreferences";

    /**
     * The type of action for the tailor reference.
     */
    TailoringReferenceType getReferenceType();

    void setReferenceType(TailoringReferenceType aReferenceType);

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return TailoringReference.class;
    }

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }
}
