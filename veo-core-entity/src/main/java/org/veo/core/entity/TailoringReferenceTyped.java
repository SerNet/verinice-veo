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

import jakarta.validation.constraints.NotNull;

/** All what provides a {@link TailoringReferenceType}. */
public interface TailoringReferenceTyped {
  /**
   * Predicate to filter {@link TailoringReferenceType#COPY} or {@link
   * TailoringReferenceType#COPY_ALWAYS}.
   */
  Predicate<? super TailoringReferenceTyped> IS_COPY_PREDICATE =
      r ->
          r.getReferenceType() == TailoringReferenceType.COPY
              || r.getReferenceType() == TailoringReferenceType.COPY_ALWAYS;

  Predicate<? super TailoringReferenceTyped> IS_ALL_LINK_PREDICATE =
      r ->
          r.getReferenceType() == TailoringReferenceType.LINK
              || r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL;

  Predicate<? super TailoringReferenceTyped> IS_EXTERNALLINK_PREDICATE =
      r -> r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL;

  /** Predicate to filter {@link TailoringReferenceType#LINK} */
  Predicate<? super TailoringReferenceTyped> IS_LINK_PREDICATE =
      r -> r.getReferenceType() == TailoringReferenceType.LINK;

  Predicate<? super TailoringReferenceTyped> IS_ALL_PART_PREDICATE =
      r ->
          r.getReferenceType() == TailoringReferenceType.PART
              || r.getReferenceType() == TailoringReferenceType.COMPOSITE;

  /** Predicate to filter {@link TailoringReferenceType#PART} */
  Predicate<? super TailoringReferenceTyped> IS_PART_PREDICATE =
      r -> r.getReferenceType() == TailoringReferenceType.PART;

  /** Predicate to filter {@link TailoringReferenceType#COMPOSITE} */
  Predicate<? super TailoringReferenceTyped> IS_COMPOSITE_PREDICATE =
      r -> r.getReferenceType() == TailoringReferenceType.COMPOSITE;

  /** Predicate to filter all tailorref mapping to a parameter */
  Predicate<? super TailoringReferenceTyped> IS_PARAMETER_REF =
      r ->
          r.getReferenceType() == TailoringReferenceType.LINK
              || r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL
              || r.getReferenceType() == TailoringReferenceType.PART
              || r.getReferenceType() == TailoringReferenceType.COMPOSITE;

  /** Is any kind of link References. */
  default boolean isLinkTailoringReferences() {
    return IS_ALL_LINK_PREDICATE.test(this);
  }

  @NotNull
  TailoringReferenceType getReferenceType();
}
