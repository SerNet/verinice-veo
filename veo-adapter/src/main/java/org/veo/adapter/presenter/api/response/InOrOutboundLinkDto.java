/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.adapter.presenter.api.response;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.InOrOutboundLink;
import org.veo.core.entity.LinkDirection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public class InOrOutboundLinkDto {
  LinkDirection direction;
  String linkType;
  ElementInDomainIdRef<Element> linkedElement;

  public static InOrOutboundLinkDto from(
      InOrOutboundLink source, Domain domain, ReferenceAssembler referenceAssembler) {
    return new InOrOutboundLinkDto(
        source.direction(),
        source.linkType(),
        ElementInDomainIdRef.from(source.linkedElement(), domain, referenceAssembler));
  }
}
