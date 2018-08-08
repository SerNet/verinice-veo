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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides method to validate a JSON against a JSON schema.
 */
public class JsonValidator {

    private JsonSchema schema;

    public JsonValidator(InputStream is) throws IOException, ProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metaSchemaNode = mapper.readTree(is);
        try {
            final URITranslatorConfiguration uriCfg = URITranslatorConfiguration.newBuilder()
                    .setNamespace("http://verinice.com/veo/")
                    .addSchemaRedirect("http://verinice.com/veo/draft-01/schema",
                            "resource:/meta.json")
                    .addSchemaRedirect("https://verinice.com/veo/draft-01/schema",
                            "resource:/meta.json").freeze();
            final LoadingConfiguration loadingCfg = LoadingConfiguration.newBuilder()
                    .setURITranslatorConfiguration(uriCfg).freeze();
            final JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
                    .setLoadingConfiguration(loadingCfg).freeze();

            schema = factory.getJsonSchema(metaSchemaNode);
        } catch (ProcessingException e) {
            throw new ProcessingException("error while validating schema", e);
        }
    }

    public ValidationResult validate(InputStream schemaStream)
            throws IOException, ProcessingException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode schemaMap = mapper.readTree(schemaStream);
            return validate(schemaMap);
        } catch (ProcessingException e) {
            throw new ProcessingException("error while validating schema", e);
        }
    }

    public ValidationResult validate(JsonNode schemaMap)
            throws ProcessingException {
        try {
            return new ValidationResult(schema.validate(schemaMap));
        } catch (ProcessingException e) {
            throw new ProcessingException("error while validating schema", e);
        }
    }
}
