/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
package org.veo.adapter.persistence.schema;

import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generates JSON schemas for java classes. Returned Schemas are encapsulated in {@link Supplier}
 * objects that may be loaded eagerly when the application starts. Each time the actual schema is
 * accessed on a supplier, a deep copy is created, so the original encapsulated schema is immutable.
 */
public class SchemaProvider {
  private static final SchemaProvider INSTANCE = new SchemaProvider();
  private final SchemaGenerator schemaGenerator = createSchemaGenerator();

  public static SchemaProvider getInstance() {
    return INSTANCE;
  }

  /** Generates JSON schema for given class. */
  public Supplier<ObjectNode> schema(Class<?> clazz) {
    var schema = schemaGenerator.generateSchema(clazz);
    return schema::deepCopy;
  }

  private SchemaGenerator createSchemaGenerator() {
    JacksonModule jacksonModule = new JacksonModule();
    JakartaValidationModule jakartaValidationModule =
        new JakartaValidationModule(JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED);
    Module swagger2Module = new Swagger2Module();

    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
            .with(jacksonModule)
            .with(swagger2Module)
            .with(jakartaValidationModule)
            .with(
                Option.INLINE_ALL_SCHEMAS,
                Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
                Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS);
    configBuilder
        .forMethods()
        .withIgnoreCheck(
            method ->
                method.getAnnotation(JsonProperty.class) == null
                    && method.getAnnotation(Schema.class) == null);

    SchemaGeneratorConfig config = configBuilder.build();
    return new SchemaGenerator(config);
  }
}
