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
package org.veo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonProperty;
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

import io.swagger.v3.oas.annotations.media.Schema;

public class GenerateEntitySchema {

    public static void main(String[] args) throws Exception {
        String outputBaseDir = args[0];
        Path outputExendedSchemas = Paths.get(outputBaseDir, "entity");
        String snippetDir = args[1];
        String packageName = args[2];
        CustomPrettyPrinter prettyPrinter = new CustomPrettyPrinter();
        prettyPrinter.indentArraysWith(new DefaultIndenter());
        ObjectWriter writer = new ObjectMapper().writer(prettyPrinter);

        SchemaGenerator generator = createSchemaGenerator();
        SchemaMerger schemaMerger = new SchemaMerger(Paths.get(snippetDir));

        for (int i = 3; i < args.length; i++) {
            String baseName = args[i];
            Class<?> clazz = Class.forName(packageName + ".Full" + baseName + "Dto");

            JsonNode jsonSchema = generator.generateSchema(clazz);

            schemaMerger.extendSchema(jsonSchema, baseName);

            writer.writeValue(Files.createDirectories(outputExendedSchemas)
                                   .resolve(jsonSchema.get("title")
                                                      .asText()
                                           + ".json")
                                   .toFile(),
                              jsonSchema);
        }
    }

    protected static SchemaGenerator createSchemaGenerator() {
        JacksonModule jacksonModule = new JacksonModule();
        JavaxValidationModule javaxValidationModule = new JavaxValidationModule(
                JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED);
        Module swagger2Module = new Swagger2Module();

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON).with(jacksonModule)
                                                               .with(swagger2Module)
                                                               .with(javaxValidationModule)
                                                               .with(Option.INLINE_ALL_SCHEMAS,
                                                                     Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
                                                                     Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS);
        configBuilder.forMethods()
                     .withIgnoreCheck(method -> method.getAnnotation(JsonProperty.class) == null
                             && method.getAnnotation(Schema.class) == null);

        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config);
    }
}