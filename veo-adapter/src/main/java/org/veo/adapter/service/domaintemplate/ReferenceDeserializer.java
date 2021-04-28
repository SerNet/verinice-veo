/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.ModelObjectType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ReferenceDeserializer extends JsonDeserializer<SyntheticModelObjectReference<?>> {
    private static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    public static final String TARGET_URI = "targetUri";
    private final ReferenceAssembler urlAssembler;

    public ReferenceDeserializer(ReferenceAssembler urlAssembler) {
        this.urlAssembler = urlAssembler;
    }

    @Override
    public SyntheticModelObjectReference<?> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        TreeNode treeNode = p.getCodec()
                             .readTree(p);
        TextNode targetUri = (TextNode) treeNode.get(TARGET_URI);
        String asText = targetUri.asText();

        Pattern compile = Pattern.compile(".*\\/([a-z]*)\\/(" + UUID_REGEX + ")");

        Matcher matcher = compile.matcher(asText);
        if (matcher.matches()) {
            String term = matcher.group(1);
            Class<ModelObject> type = (Class<ModelObject>) ModelObjectType.getTypeForPluralTerm(term);
            String id = matcher.group(2);
            return new SyntheticModelObjectReference<ModelObject>(id, type, urlAssembler);
        }

        return null;
    }

}
