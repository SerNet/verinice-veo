/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.usecase.catalogitem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.veo.core.entity.Element;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.exception.RuntimeModelException;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;

public class AbstractGetIncarnationDescriptionUseCase<T extends TemplateItem<T>> {

  protected List<TailoringReferenceParameter> toParameters(
      Collection<TailoringReference<T>> catalogItem, Map<T, Optional<Element>> itemsToElements) {
    return catalogItem.stream()
        .filter(TailoringReferenceTyped.IS_PARAMETER_REF)
        .map(
            tr ->
                mapParameter(
                    tr,
                    Optional.ofNullable(itemsToElements.get(tr.getTarget()))
                        .flatMap(Function.identity())
                        .orElse(null)))
        .toList();
  }

  private TailoringReferenceParameter mapParameter(
      TailoringReference<T> reference, Element element) {
    return switch (reference.getReferenceType()) {
      case PART, COMPOSITE, RISK, SCOPE, MEMBER -> fromReference(reference, element);
      case LINK, LINK_EXTERNAL -> fromLinkReference((LinkTailoringReference<T>) reference, element);
      default ->
          throw new IllegalArgumentException(
              "Unmapped tailoring reference type: " + reference.getReferenceType());
    };
  }

  private TailoringReferenceParameter fromReference(
      TailoringReference<T> linkReference, Element element) {
    TailoringReferenceParameter tailoringReferenceParameter =
        new TailoringReferenceParameter(linkReference.getReferenceType(), null);
    tailoringReferenceParameter.setReferencedElement(element);
    tailoringReferenceParameter.setId(linkReference.getId().uuidValue());
    return tailoringReferenceParameter;
  }

  /** Create a parameter object for this {@link LinkTailoringReference}. */
  private TailoringReferenceParameter fromLinkReference(
      LinkTailoringReference<T> linkReference, Element element) {
    if (linkReference.getLinkType() == null) {
      throw new RuntimeModelException(
          "LinkType should not be null affected TailoringReferences: " + linkReference.getId());
    }
    TailoringReferenceParameter tailoringReferenceParameter =
        new TailoringReferenceParameter(
            linkReference.getReferenceType(), linkReference.getLinkType());
    tailoringReferenceParameter.setId(linkReference.getId().uuidValue());
    tailoringReferenceParameter.setReferencedElement(element);
    return tailoringReferenceParameter;
  }

  Predicate<TailoringReferenceTyped> createTailoringReferenceFilter(
      List<TailoringReferenceType> exclude, List<TailoringReferenceType> include) {
    Predicate<TailoringReferenceTyped> filter = t -> true;
    if (exclude != null && !exclude.isEmpty()) {
      filter = t -> !exclude.contains(t.getReferenceType());
    }
    if (include != null && !include.isEmpty()) {
      filter = filter.and(t -> include.contains(t.getReferenceType()));
    }
    return filter;
  }
}
