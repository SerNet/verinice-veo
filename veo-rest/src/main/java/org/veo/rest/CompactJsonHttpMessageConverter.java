/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.rest;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uses a custom JSON media type, producing a more lightweight JSON output where read-only
 * properties and properties with default values are omitted.
 */
public class CompactJsonHttpMessageConverter extends AbstractJackson2HttpMessageConverter {
  public static final MediaType MEDIA_TYPE_JSON_COMPACT =
      MediaType.parseMediaType("application/vnd.sernet.verinice.compact+json");

  public CompactJsonHttpMessageConverter(ObjectMapper defaultMapper) {
    super(configureCompactMapper(defaultMapper), MEDIA_TYPE_JSON_COMPACT);
  }

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private class Dummy {}

  private static ObjectMapper configureCompactMapper(ObjectMapper defaultMapper) {
    var includeNonDefault = new JsonInclude.Value(Dummy.class.getAnnotation(JsonInclude.class));
    return defaultMapper
        .copy()
        // Omit properties where the value equals the default value as defined in the DTO class.
        // setDefaultPropertyInclusion may seem like the obvious solution, but NON_DEFAULT does not
        // behave as desired when used with setDefaultPropertyInclusion.
        // https://fasterxml.github.io/jackson-annotations/javadoc/2.7/com/fasterxml/jackson/annotation/JsonInclude.Include.html#NON_DEFAULT
        // https://stackoverflow.com/questions/68637273/jackson-objectmapper-custom-global-filter-usage
        .setConfig(
            defaultMapper
                .getSerializationConfig()
                .withAppendedAnnotationIntrospector(
                    new AnnotationIntrospector() {
                      @Override
                      public Version version() {
                        return null;
                      }

                      @Override
                      public JsonInclude.Value findPropertyInclusion(Annotated a) {
                        if (a instanceof AnnotatedClass m && m.getRawType().isRecord()) {
                          // NON_DEFAULT makes no sense for records, because jackson cannot apply
                          // default values when deserializing a record:
                          // https://github.com/FasterXML/jackson-future-ideas/issues/67
                          return null;
                        }
                        return includeNonDefault;
                      }

                      /** Omit read-only properties */
                      @Override
                      public boolean hasIgnoreMarker(AnnotatedMember m) {
                        if (super.hasIgnoreMarker(m)) {
                          return true;
                        }
                        var propAnn = m.getAnnotation(JsonProperty.class);
                        if (propAnn != null && propAnn.access() == JsonProperty.Access.READ_ONLY) {
                          return true;
                        }
                        var schemaAnn = m.getAnnotation(Schema.class);
                        if (schemaAnn != null
                            && schemaAnn.accessMode() == Schema.AccessMode.READ_ONLY) {
                          return true;
                        }
                        return false;
                      }
                    }));
  }
}
