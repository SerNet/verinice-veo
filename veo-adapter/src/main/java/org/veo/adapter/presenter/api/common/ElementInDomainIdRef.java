/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.adapter.presenter.api.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/** Reference to an element within a specific domain, based on element ID & domain ID. */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ElementInDomainIdRef<TElement extends Element> extends IdRef<TElement> {
  private final TElement element;
  private final Domain domain;

  @Schema(description = "Sub type that the target element has in this domain")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Getter
  private final String subType;

  protected ElementInDomainIdRef(
      String id,
      Domain domain,
      String displayName,
      Class<TElement> type,
      ReferenceAssembler urlAssembler,
      String uri,
      TElement element,
      String subType) {
    super(id, displayName, type, urlAssembler, uri, element);
    this.element = element;
    this.domain = domain;
    this.subType = subType;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getTargetInDomainUri() {
    return urlAssembler.elementInDomainRefOf(element, domain);
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean getAssociatedWithDomain() {
    return subType != null;
  }

  public static <T extends Element> ElementInDomainIdRef<T> from(
      T element, Domain domain, @NonNull ReferenceAssembler urlAssembler) {
    if (element == null) {
      return null;
    }
    return new ElementInDomainIdRef<>(
        element.getIdAsString(),
        domain,
        element.getDisplayName(),
        (Class<T>) element.getModelInterface(),
        urlAssembler,
        null,
        element,
        element.getSubType(domain).orElse(null));
  }
}
