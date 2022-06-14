/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase.base;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;

/** Validates catalog items according to the domain's element type definitions. */
public class CatalogItemValidator {
  public static void validate(CatalogItem item) {
    try {
      DomainSensitiveElementValidator.validate(item.getElement());
      var domain = item.getCatalog().getDomainTemplate();
      item.getTailoringReferences()
          .forEach(tailoringReference -> validate(tailoringReference, domain));
    } catch (IllegalArgumentException illEx) {
      throw new IllegalArgumentException(
          String.format(
              "Illegal catalog item '%s' in catalog '%s': %s",
              item.getElement().getName(), item.getCatalog().getDisplayName(), illEx.getMessage()),
          illEx);
    }
  }

  private static void validate(TailoringReference tailoringReference, DomainTemplate domain) {
    var item = tailoringReference.getOwner();
    var referenceType = tailoringReference.getReferenceType();
    if (referenceType == TailoringReferenceType.LINK) {
      var linkRef = (LinkTailoringReference) tailoringReference;
      DomainSensitiveElementValidator.validateLink(
          linkRef.getLinkType(),
          item.getElement(),
          linkRef.getCatalogItem().getElement(),
          linkRef.getAttributes(),
          domain);
    } else if (referenceType == TailoringReferenceType.LINK_EXTERNAL) {
      var linkRef = (LinkTailoringReference) tailoringReference;
      DomainSensitiveElementValidator.validateLink(
          linkRef.getLinkType(),
          linkRef.getCatalogItem().getElement(),
          item.getElement(),
          linkRef.getAttributes(),
          domain);
    }
  }
}
