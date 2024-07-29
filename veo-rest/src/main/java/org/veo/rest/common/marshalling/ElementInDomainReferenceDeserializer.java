/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.rest.common.marshalling;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.exception.UnprocessableDataException;

/**
 * Deserializes resource references from JSON. Uses {@link ReferenceAssembler} to deconstruct URLs.
 */
@JsonComponent
public class ElementInDomainReferenceDeserializer
    extends JsonDeserializer<ElementInDomainIdRef<?>> {

  public static final String TARGET_URI = "targetUri";
  public static final String TARGET_IN_DOMAIN_URI = "targetInDomainUri";

  @Autowired ReferenceAssembler urlAssembler;

  @Override
  public ElementInDomainIdRef<?> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    var treeNode = p.getCodec().readTree(p);
    var targetUri = treeNode.get(TARGET_URI);
    if (targetUri != null) {
      return ElementInDomainIdRef.fromTargetUri(((TextNode) targetUri).asText(), urlAssembler);
    }
    var targetInDomain = treeNode.get(TARGET_IN_DOMAIN_URI);
    if (targetInDomain != null) {
      return ElementInDomainIdRef.fromTargetUri(((TextNode) targetInDomain).asText(), urlAssembler);
    }
    throw new UnprocessableDataException(
        "Element reference must contain %s or %s".formatted(TARGET_URI, TARGET_IN_DOMAIN_URI));
  }
}
