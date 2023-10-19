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

import java.util.Map;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.definitions.LinkDefinition;

/** Validates template items according to the domain's element type definitions. */
public class TemplateItemValidator {
  public static <T extends TemplateItem<T>> void validate(T item) {
    var domain = item.getDomainBase();
    SubTypeValidator.validate(domain, item.getSubType(), item.getStatus(), item.getElementType());
    item.getCustomAspects()
        .forEach(
            (caType, customAspects) -> {
              var caDefinition =
                  domain
                      .getElementTypeDefinition(item.getElementType())
                      .getCustomAspectDefinition(caType);
              AttributeValidator.validate(customAspects, caDefinition.getAttributeDefinitions());
            });

    item.getTailoringReferences().forEach(tr -> validate(tr, domain));
  }

  public static <T extends TemplateItem<T>> void validate(
      TailoringReference<T> tailoringReference, DomainBase domain) {
    if (tailoringReference instanceof LinkTailoringReference<T> linkRef) {
      validateLink(
          linkRef.getLinkType(),
          linkRef.getLinkSourceItem(),
          linkRef.getLinkTargetItem(),
          linkRef.getAttributes(),
          domain);
    }
  }

  private static <T extends TemplateItem<T>> void validateLink(
      String linkType,
      T linkSourceItem,
      T linkTargetItem,
      Map<String, Object> attributes,
      DomainBase domain) {
    var linkDefinition =
        domain.getElementTypeDefinition(linkSourceItem.getElementType()).getLinks().get(linkType);
    if (linkDefinition == null) {
      throw new IllegalArgumentException(
          String.format(
              "Link type '%s' is not defined for element type '%s'",
              linkType, linkSourceItem.getElementType()));
    }
    validateLinkTargetType(linkType, linkTargetItem, linkDefinition);
    validateLinkTargetSubType(linkType, linkTargetItem, linkDefinition);
    try {
      AttributeValidator.validate(attributes, linkDefinition.getAttributeDefinitions());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          String.format("Invalid attributes for link type '%s': %s", linkType, ex.getMessage()),
          ex);
    }
  }

  private static <T extends TemplateItem<T>> void validateLinkTargetType(
      String linkType, T target, LinkDefinition linkDefinition) {
    var targetType = target.getElementType();
    DomainSensitiveElementValidator.validateLinkTargetType(linkType, linkDefinition, targetType);
  }

  private static <T extends TemplateItem<T>> void validateLinkTargetSubType(
      String linkType, T target, LinkDefinition linkDefinition) {
    var targetSubType = target.getSubType();
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      throw new IllegalArgumentException(
          String.format(
              "Expected target of link '%s' ('%s') to have sub type '%s' but found '%s'",
              linkType, target.getName(), linkDefinition.getTargetSubType(), targetSubType));
    }
  }
}
