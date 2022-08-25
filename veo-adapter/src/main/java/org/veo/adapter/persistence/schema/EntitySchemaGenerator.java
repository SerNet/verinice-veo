/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

import org.veo.core.entity.Domain;
import org.veo.core.entity.EntitySchemaException;
import org.veo.core.entity.EntityType;

import io.swagger.v3.oas.annotations.media.Schema;

public class EntitySchemaGenerator {

  private static final String PACKAGE_NAME = "org.veo.adapter.presenter.api.dto.full";

  private final SchemaExtender schemaExtender;
  private final Map<String, JsonNode> baseSchemas;

  private final ObjectWriter writer;

  public EntitySchemaGenerator(SchemaExtender schemaExtender) {
    this.schemaExtender = schemaExtender;
    SchemaGenerator generator = createSchemaGenerator();

    CustomPrettyPrinter prettyPrinter = new CustomPrettyPrinter();
    prettyPrinter.indentArraysWith(new DefaultIndenter());
    writer = new ObjectMapper().writer(prettyPrinter);
    baseSchemas =
        EntityType.ELEMENT_TYPES.stream()
            .map(EntityType::getSingularTerm)
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    singularTerm -> {
                      try {
                        Class<?> clazz =
                            Class.forName(
                                PACKAGE_NAME + ".Full" + capitalize(singularTerm) + "Dto");
                        return generator.generateSchema(clazz);
                      } catch (ClassNotFoundException e) {
                        throw new EntitySchemaException(
                            "Error initializing base schema for " + singularTerm, e);
                      }
                    }));
  }

  public String createSchema(String baseName, Set<Domain> domains) {
    JsonNode jsonSchema = baseSchemas.get(baseName);
    if (jsonSchema == null) {
      throw new EntitySchemaException("Invalid entity type: " + baseName);
    }

    try {
      jsonSchema = jsonSchema.deepCopy();
      SchemaGenerator generator = createSchemaGenerator();
      schemaExtender.extendSchema(generator, jsonSchema, baseName, domains);

      return writer.writeValueAsString(jsonSchema);
    } catch (JsonProcessingException e) {
      throw new EntitySchemaException("Schema creation failed", e);
    }
  }

  protected static SchemaGenerator createSchemaGenerator() {
    JacksonModule jacksonModule = new JacksonModule();
    JavaxValidationModule javaxValidationModule =
        new JavaxValidationModule(JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED);
    Module swagger2Module = new Swagger2Module();

    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
            .with(jacksonModule)
            .with(swagger2Module)
            .with(javaxValidationModule)
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
