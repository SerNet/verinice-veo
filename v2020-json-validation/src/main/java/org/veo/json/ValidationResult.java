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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class bundles information of JSON validation, e.g. success, warnings, errors etc.
 */
public class ValidationResult {

    private boolean successful;
    private Collection<JsonNode> messages;

    ValidationResult(ProcessingReport report) {
        this.successful = report.isSuccess();
        this.messages = StreamSupport.stream(report.spliterator(), false)
                .map(ProcessingMessage::asJson).collect(Collectors.toList());
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
    public Collection<JsonNode> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(successful ? "successful\n" : "unsuccessful\n");
        sb.append('[');
        messages.forEach(message -> sb.append(message.toString()).append(','));
        sb.append(']');
        return sb.toString();
    }
}
