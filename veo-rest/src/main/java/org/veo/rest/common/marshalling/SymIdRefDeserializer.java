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
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.SymIdRef;

/**
 * Deserializes resource references from JSON. Uses {@link ReferenceAssembler} to deconstruct URLs.
 */
@JsonComponent
public class SymIdRefDeserializer extends JsonDeserializer<SymIdRef<?, ?>> {

  public static final String TARGET_URI = "targetUri";

  @Autowired ReferenceAssembler urlAssembler;

  @Override
  public SymIdRef<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    TreeNode treeNode = p.getCodec().readTree(p);
    TextNode targetUri = (TextNode) treeNode.get(TARGET_URI);
    return SymIdRef.fromUri(targetUri.asText(), urlAssembler);
  }
}
