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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;

import tools.jackson.core.JsonParser;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.StringNode;

/**
 * Deserializes resource references from JSON. Uses {@link ReferenceAssembler} to deconstruct URLs.
 */
@JacksonComponent
public class ReferenceDeserializer extends ValueDeserializer<IdRef<?>> {

  public static final String TARGET_URI = "targetUri";

  @Autowired ReferenceAssembler urlAssembler;

  @Override
  public IdRef<?> deserialize(JsonParser p, DeserializationContext ctxt) {
    TreeNode treeNode = p.readValueAsTree();
    StringNode targetUri = (StringNode) treeNode.get(TARGET_URI);
    return IdRef.fromUri(targetUri.asText(), urlAssembler);
  }
}
