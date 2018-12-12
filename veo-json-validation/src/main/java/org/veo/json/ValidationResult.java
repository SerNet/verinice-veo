/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Alexander Ben Nasrallah <an@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

/**
 * This class bundles information of JSON validation, e.g. success, warnings, errors etc.
 */
public class ValidationResult {

    private static final JsonNodeFactory FACTORY = JacksonUtils.nodeFactory();

    private boolean successful;
    private ArrayNode messages;

    ValidationResult(ProcessingReport report) {
        this.successful = report.isSuccess();

        messages = FACTORY.arrayNode();
        for (final ProcessingMessage message: report)
            messages.add(message.asJson());
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * A message is a JsonNode matching the schema
     *
     * {@code
     *     {
     *         "description": "schema for a ValidationMessage's JSON representation",
     *         "type": "object",
     *         "properties": {
     *             "domain": {
     *                 "enum": [ "$ref resolving", "syntax", "validation", "unknown" ],
     *                 "required": true
     *             },
     *             "keyword": {
     *                 "type": "string",
     *                 "required": true
     *             },
     *             "message": {
     *                 "type": "string",
     *                 "required": true
     *             }
     *         }
     *     }
     * }
     */
    public ArrayNode getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return String.join("\n", successful ? "successful" : "unsuccessful", messages.toString());
    }
}
