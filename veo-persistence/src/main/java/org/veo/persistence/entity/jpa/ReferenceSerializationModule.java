/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.persistence.entity.jpa;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.core.entity.risk.ImpactReason;

public class ReferenceSerializationModule extends SimpleModule {

  private static final long serialVersionUID = 7359286298153893375L;

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  public ReferenceSerializationModule() {

    addKeySerializer(
        Locale.class,
        new JsonSerializer<>() {
          @Override
          public void serialize(Locale value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeFieldName(value.toLanguageTag());
          }
        });
    addKeyDeserializer(
        Locale.class,
        new KeyDeserializer() {
          @Override
          public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            return Locale.forLanguageTag(key);
          }
        });
    addSerializer(
        Locale.class,
        new JsonSerializer<>() {
          @Override
          public void serialize(Locale value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeFieldName(value.toLanguageTag());
          }
        });

    addDeserializer(
        Locale.class,
        new JsonDeserializer<>() {
          @Override
          public Locale deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Locale.forLanguageTag(p.getValueAsString());
          }
        });

    addSerializer(
        ImpactReason.class,
        new JsonSerializer<>() {
          @Override
          public void serialize(
              ImpactReason reason, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeString(reason.getTranslationKey());
          }
        });
    addDeserializer(
        ImpactReason.class,
        new JsonDeserializer<>() {
          @Override
          public ImpactReason deserialize(JsonParser p, DeserializationContext ctxt)
              throws IOException {
            return ImpactReason.fromTranslationKey(p.getValueAsString());
          }
        });
  }
}
