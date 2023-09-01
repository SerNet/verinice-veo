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

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.definitions.LinkDefinition;

/** Validates catalog items according to the domain's element type definitions. */
public class CatalogItemValidator {

  public static void validate(CatalogItem item) {
    DomainBase domain = item.getOwner();
    SubTypeValidator.validate(domain, item.getSubType(), item.getStatus(), item.getElementType());
    item.getCustomAspects().entrySet().stream()
        .forEach(
            e -> {
              var caDefinition =
                  domain
                      .getElementTypeDefinition(item.getElementType())
                      .getCustomAspectDefinition(e.getKey());
              AttributeValidator.validate(e.getValue(), caDefinition.getAttributeDefinitions());
            });

    item.getTailoringReferences().stream().forEach(tr -> validate(tr, domain));
  }

  public static void validate(TailoringReference tailoringReference, DomainBase domain) {
    if (tailoringReference instanceof LinkTailoringReference linkRef) {
      validateLink(
          linkRef.getLinkType(),
          linkRef.getLinkSourceItem(),
          linkRef.getLinkTargetItem(),
          linkRef.getAttributes(),
          domain);
    }
  }

  private static void validateLink(
      String linkType,
      CatalogItem linkSourceItem,
      CatalogItem linkTargetItem,
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

  private static void validateLinkTargetType(
      String linkType, CatalogItem target, LinkDefinition linkDefinition) {
    var targetType = target.getElementType();
    DomainSensitiveElementValidator.validateLinkTargetType(linkType, linkDefinition, targetType);
  }

  private static void validateLinkTargetSubType(
      String linkType, CatalogItem target, LinkDefinition linkDefinition) {
    var targetSubType = target.getSubType();
    if (!linkDefinition.getTargetSubType().equals(targetSubType)) {
      throw new IllegalArgumentException(
          String.format(
              "Expected target of link '%s' ('%s') to have sub type '%s' but found '%s'",
              linkType, target.getName(), linkDefinition.getTargetSubType(), targetSubType));
    }
  }
}
