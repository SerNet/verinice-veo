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

/**
 * Actiontype for {@link TailoringReference}. It defines the kind of actions a {@link
 * TailoringReference} can define.
 */
public enum TailoringReferenceType {
  /** Defines an action to skip this {@link TailoringReference}. */
  OMIT,
  /**
   * Defines a {@link TailoringReference} which corresponds to a {@link CustomLink} or a {@link
   * CompositeElement} in the element of the owning {@link CatalogItem}. The target of the feature
   * need to be the element owned by the target of the {@link TailoringReference#getTarget()}.
   */
  LINK,
  /**
   * Defines a link in another {@link Element} which points to the {@link CatalogItem#getElement()}.
   * The property {@link TailoringReference#getTarget()} points to the {@link CatalogItem} in which
   * the {@link ExternalTailoringReference#getExternalLink()} object is added. This describes the
   * modeling of an opposite feature.
   */
  LINK_EXTERNAL,
  /**
   * Create the linked {@link CatalogItem#getElement()} in the same step. It is a bit vage in the
   * sense when to create.
   */
  COPY,
  /**
   * @see TailoringReferenceType#COPY, but clear in semantics, it alway create the linked {@link
   *     CatalogItem#getElement()} in the same step.
   */
  COPY_ALWAYS,
  /**
   * Defines this references as part of a composite. The owner is the part and the {@link
   * TailoringReference#getTarget()} is the composite.
   */
  PART,
  /**
   * Defines this references as part of a composite. The owner is the composite and the {@link
   * TailoringReference#getTarget()} is the part. This is the opposite feature to {@link #PART}.
   */
  COMPOSITE,
  /**
   * Defines a risk as part of a {@link RiskAffected} in a {@link ProfileItem} and maps to a {@link
   * RiskTailoringReference}. It describes relations to {@link Scenario}, {@link Control} an {@link
   * Person} and the risk data.
   */
  RISK,
  /**
   * Defines this reference as part of a scope relation. The owner is the scope and the {@link
   * TailoringReference#getTarget()} is the member.
   */
  SCOPE,
  /**
   * Defines this reference as part of a scope relation. The owner is the member and the {@link
   * TailoringReference#getTarget()} is the scope.
   */
  MEMBER,
  /**
   * Defines a control implementation as part of a {@link RiskAffected} in a {@link ProfileItem} and
   * maps to a {@link ControlImplementationTailoringReference}. It targets a {@link TemplateItem}
   * representing a {@link Control}.
   */
  CONTROL_IMPLEMENTATION,
  /**
   * Defines a requirement implementation as part of a {@link RiskAffected} in a {@link ProfileItem}
   * and maps to a {@link RequirementImplementationTailoringReference}. It targets a {@link
   * TemplateItem} representing a {@link Control}.
   */
  REQUIREMENT_IMPLEMENTATION;
}
