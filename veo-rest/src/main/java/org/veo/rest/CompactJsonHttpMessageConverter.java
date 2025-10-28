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
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;

/**
 * Uses a custom JSON media type, producing a more lightweight JSON output where read-only
 * properties and properties with default values are omitted.
 */
public class CompactJsonHttpMessageConverter extends AbstractJacksonHttpMessageConverter {
  public static final MediaType MEDIA_TYPE_JSON_COMPACT =
      MediaType.parseMediaType("application/vnd.sernet.verinice.compact+json");

  public CompactJsonHttpMessageConverter(ObjectMapper defaultMapper) {
    super(configureCompactMapper(defaultMapper), MEDIA_TYPE_JSON_COMPACT);
  }

  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private final class Dummy {}

  private static ObjectMapper configureCompactMapper(ObjectMapper defaultMapper) {
    var includeNonDefault = new JsonInclude.Value(Dummy.class.getAnnotation(JsonInclude.class));
    var defaultIntrospector = defaultMapper.serializationConfig().getAnnotationIntrospector();

    // Omit properties where the value equals the default value as defined in the DTO class.
    // setDefaultPropertyInclusion may seem like the obvious solution, but NON_DEFAULT does not
    // behave as desired when used with setDefaultPropertyInclusion.
    // https://fasterxml.github.io/jackson-annotations/javadoc/2.7/com/fasterxml/jackson/annotation/JsonInclude.Include.html#NON_DEFAULT
    // https://stackoverflow.com/questions/68637273/jackson-objectmapper-custom-global-filter-usage

    var newIntrospector =
        AnnotationIntrospectorPair.create(
            defaultIntrospector,
            new AnnotationIntrospector() {
              @Override
              public Version version() {
                return null;
              }

              @Override
              public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
                if (a instanceof AnnotatedClass m && m.getRawType().isRecord()) {
                  // NON_DEFAULT makes no sense for records, because jackson cannot apply
                  // default values when deserializing a record:
                  // https://github.com/FasterXML/jackson-future-ideas/issues/67
                  return null;
                }
                return includeNonDefault;
              }

              @Override
              public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember member) {
                if (super.hasIgnoreMarker(config, member)) {
                  return true;
                }
                var propAnn = member.getAnnotation(JsonProperty.class);
                if (propAnn != null && propAnn.access() == JsonProperty.Access.READ_ONLY) {
                  return true;
                }
                var schemaAnn = member.getAnnotation(Schema.class);
                if (schemaAnn != null && schemaAnn.accessMode() == Schema.AccessMode.READ_ONLY) {
                  return true;
                }
                return false;
              }
            });
    return defaultMapper.rebuild().annotationIntrospector(newIntrospector).build();
  }
}
