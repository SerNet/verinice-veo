/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.persistence;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.core.entity.risk.ImplementationStatusRef;
import org.veo.core.entity.risk.RiskDefinitionRef;

/**
 * Custom mapping for use in JSONB DB columns.
 */
public class CustomObjectMapper extends ObjectMapper {
    public CustomObjectMapper() {
        registerModule(new RefModule());
    }

    private class RefModule extends SimpleModule {
        public RefModule() {
            addSerializer(ImplementationStatusRef.class, new JsonSerializer<>() {
                @Override
                public void serialize(ImplementationStatusRef value, JsonGenerator gen,
                        SerializerProvider serializers) throws IOException {
                    gen.writeString(value.getKey());
                }
            });
            addDeserializer(ImplementationStatusRef.class,
                            new JsonDeserializer<ImplementationStatusRef>() {
                                @Override
                                public ImplementationStatusRef deserialize(JsonParser p,
                                        DeserializationContext ctxt)
                                        throws IOException, JacksonException {
                                    return Optional.ofNullable(p.getText())
                                                   .map(key -> ImplementationStatusRef.from(key))
                                                   .orElse(null);
                                }
                            });

            addSerializer(RiskDefinitionRef.class, new JsonSerializer<>() {
                @Override
                public void serialize(RiskDefinitionRef value, JsonGenerator gen,
                        SerializerProvider serializers) throws IOException {
                    gen.writeString(value.getId());
                }
            });
            addDeserializer(RiskDefinitionRef.class, new JsonDeserializer<RiskDefinitionRef>() {
                @Override
                public RiskDefinitionRef deserialize(JsonParser p, DeserializationContext ctxt)
                        throws IOException, JacksonException {
                    return Optional.ofNullable(p.getText())
                                   .map(key -> RiskDefinitionRef.from(key))
                                   .orElse(null);
                }
            });
            addKeySerializer(RiskDefinitionRef.class, new JsonSerializer<>() {
                @Override
                public void serialize(RiskDefinitionRef value, JsonGenerator gen,
                        SerializerProvider serializers) throws IOException {
                    gen.writeFieldName(value.getId());
                }
            });
            addKeyDeserializer(RiskDefinitionRef.class, new KeyDeserializer() {
                @Override
                public Object deserializeKey(String key, DeserializationContext ctxt)
                        throws IOException {
                    return RiskDefinitionRef.from(key);
                }
            });
        }
    }
}
