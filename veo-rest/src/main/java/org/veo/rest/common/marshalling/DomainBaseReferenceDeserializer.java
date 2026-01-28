/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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

import static org.veo.rest.common.marshalling.ReferenceDeserializer.TARGET_URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;

import org.veo.adapter.presenter.api.common.DomainBaseIdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.exception.UnprocessableDataException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.StringNode;

@JacksonComponent
public class DomainBaseReferenceDeserializer extends ValueDeserializer<DomainBaseIdRef<?>> {

  @Autowired ReferenceAssembler urlAssembler;

  @Override
  public DomainBaseIdRef<?> deserialize(
      tools.jackson.core.JsonParser p, tools.jackson.databind.DeserializationContext ctxt)
      throws JacksonException {
    TreeNode treeNode = p.readValueAsTree();
    StringNode targetUri = (StringNode) treeNode.get(TARGET_URI);
    if (targetUri != null) {
      return DomainBaseIdRef.fromTargetUri(targetUri.asString(), urlAssembler);
    }
    throw new UnprocessableDataException("Domain reference must contain %s".formatted(TARGET_URI));
  }
}
