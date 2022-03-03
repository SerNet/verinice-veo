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

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.definitions.LinkDefinition;

/**
 * Validates elements considering domain-specific rules (e.g. element type
 * definitions).
 */
class DomainSensitiveElementValidator {
    public static void validate(Element element) {
        element.getLinks()
               .forEach(link -> {
                   // TODO VEO-661 use the domain that the link object is associated with (as soon
                   // as each link belongs to exactly one domain). Right now a link could also
                   // belong to zero or multiple domains, both of which are problematic.
                   var domain = element.getOwningClient()
                                       .get()
                                       .getDomains()
                                       .stream()
                                       .filter(d -> d.getElementTypeDefinition(element.getModelType())
                                                     .orElseThrow()
                                                     .getLinks()
                                                     .containsKey(link.getType()))
                                       .findAny()
                                       .orElseThrow(() -> new IllegalArgumentException(
                                               String.format("Link type '%s' is not defined in any domain used by the element.",
                                                             link.getType())));

                   var linkDefinition = domain.getElementTypeDefinition(element.getModelType())
                                              .get()
                                              .getLinks()
                                              .get(link.getType());
                   validateLinkTargetType(link, domain, linkDefinition);
                   validateLinkTargetSubType(link, domain, linkDefinition);
               });
    }

    private static void validateLinkTargetType(CustomLink link, Domain domain,
            LinkDefinition linkDefinition) {
        var targetType = link.getTarget()
                             .getModelType();
        if (!linkDefinition.getTargetType()
                           .equals(targetType)) {
            throw new IllegalArgumentException(
                    String.format("Invalid target type '%s' for link type '%s'", targetType,
                                  link.getType()));
        }
    }

    private static void validateLinkTargetSubType(CustomLink link, Domain domain,
            LinkDefinition linkDefinition) {
        if (linkDefinition.getTargetSubType() == null) {
            return;
        }
        var targetSubType = link.getTarget()
                                .getSubType(domain)
                                .orElse(null);
        if (!linkDefinition.getTargetSubType()
                           .equals(targetSubType)) {
            throw new IllegalArgumentException(
                    String.format("Invalid target sub type '%s' for link type '%s'", targetSubType,
                                  link.getType()));
        }
    }
}
