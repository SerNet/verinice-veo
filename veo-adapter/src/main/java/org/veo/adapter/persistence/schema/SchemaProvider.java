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

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

import org.veo.adapter.presenter.api.dto.ControlDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ControlRiskValuesDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ProcessRiskValuesDto;
import org.veo.adapter.presenter.api.dto.ScenarioDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ScenarioRiskValuesDto;
import org.veo.adapter.presenter.api.dto.ScopeDomainAssociationDto;
import org.veo.core.entity.EntitySchemaException;
import org.veo.core.entity.EntityType;

import io.swagger.v3.oas.annotations.media.Schema;

public class SchemaProvider {

  private static final String PACKAGE_NAME = "org.veo.adapter.presenter.api.dto.full";

  private static final SchemaProvider INSTANCE = new SchemaProvider();

  private final Map<Class<?>, ObjectNode> schemas;

  public SchemaProvider() {
    SchemaGenerator schemaGenerator = createSchemaGenerator();
    schemas =
        Stream.concat(
                EntityType.ELEMENT_TYPES.stream()
                    .map(EntityType::getSingularTerm)
                    .map(this::classForEntityName),
                Stream.of(
                    DomainAssociationDto.class,
                    ControlDomainAssociationDto.class,
                    ProcessDomainAssociationDto.class,
                    ScenarioDomainAssociationDto.class,
                    ScopeDomainAssociationDto.class,
                    ControlRiskValuesDto.class,
                    ProcessRiskValuesDto.class,
                    ScenarioRiskValuesDto.class,
                    CustomAspectDto.class,
                    CustomLinkDto.class))
            .collect(Collectors.toMap(Function.identity(), schemaGenerator::generateSchema));
  }

  public static SchemaProvider getInstance() {
    return INSTANCE;
  }

  public ObjectNode getSchema(String entitySingularName) {
    JsonNode jsonSchema = schemas.get(classForEntityName(entitySingularName));
    if (jsonSchema == null) {
      throw new IllegalArgumentException("No schema available for " + entitySingularName);
    }
    return jsonSchema.deepCopy();
  }

  private Class<?> classForEntityName(String singularTerm) {
    try {
      return Class.forName(PACKAGE_NAME + ".Full" + capitalize(singularTerm) + "Dto");
    } catch (ClassNotFoundException e) {
      throw new EntitySchemaException("Invalid entity type: " + singularTerm);
    }
  }

  public ObjectNode getSchema(Class<?> clazz) {
    JsonNode jsonSchema = schemas.get(clazz);
    if (jsonSchema == null) {
      throw new IllegalArgumentException("No schema available for " + clazz);
    }
    return jsonSchema.deepCopy();
  }

  private SchemaGenerator createSchemaGenerator() {
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
    return new SchemaGenerator(
        config,
        TypeContextFactory.createTypeContext(AnnotationInclusion.INCLUDE_AND_INHERIT, config));
  }
}
