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
package org.veo.core.entity;

import java.util.function.Predicate;

/**
 * All what provides a {@link TailoringReferenceType}.
 */
public interface TailoringReferenceTyped {
    /**
     * Predicate to filter {@link TailoringReferenceType#COPY} or
     * {@link TailoringReferenceType#COPY_ALWAYS}.
     */
    Predicate<? super TailoringReferenceTyped> IS_COPY_PREDICATE = r -> r.getReferenceType() == TailoringReferenceType.COPY
            || r.getReferenceType() == TailoringReferenceType.COPY_ALWAYS;
    Predicate<? super TailoringReferenceTyped> IS_ALL_LINK_PREDICATE = r -> r.getReferenceType() == TailoringReferenceType.LINK
            || r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL;
    Predicate<? super TailoringReferenceTyped> IS_EXTERNALLINK_PREDICATE = r -> r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL;
    /**
     * Predicate to filter {@link TailoringReferenceType#LINK}
     */
    Predicate<? super TailoringReferenceTyped> IS_LINK_PREDICATE = r -> r.getReferenceType() == TailoringReferenceType.LINK;

    TailoringReferenceType getReferenceType();
}
