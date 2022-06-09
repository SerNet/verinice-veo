/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;

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
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;
import org.veo.core.entity.riskdefinition.DiscreteValue;

/** Add domain-specific sub schemas to an element schema. */
public class SchemaExtender {
  public static final String PROPS = "properties";
  public static final String RISK_VALUES = "riskValues";
  public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
  public static final String TYPE = "type";
  private final ObjectMapper mapper = new ObjectMapper();

  public void extendSchema(
      SchemaGenerator generator, JsonNode schema, String elementType, Set<Domain> domains) {
    var domainsRoot = (ObjectNode) schema.get(PROPS).get("domains");
    var domainProps = domainsRoot.putObject(PROPS);

    var linksRoot = (ObjectNode) schema.findValue("links");
    linksRoot.put(ADDITIONAL_PROPERTIES, false);
    var linkProps = linksRoot.putObject(PROPS);

    var customAspectsSchema = (ObjectNode) schema.findValue("customAspects");
    customAspectsSchema.put(ADDITIONAL_PROPERTIES, false);
    var customObjectProperties = customAspectsSchema.putObject(PROPS);

    domains.forEach(
        domain -> {
          var domainAssociationSchema =
              addDomainAssociation(domainProps, domain, generator, elementType);
          var typeDef = domain.getElementTypeDefinition(elementType);
          addSubTypes(domainAssociationSchema, typeDef.getSubTypes());
          addCustomAspects(customObjectProperties, typeDef.getCustomAspects(), generator);
          addLinks(linkProps, typeDef.getLinks(), generator);
        });
  }

  private ObjectNode addDomainAssociation(
      ObjectNode domainProps, Domain domain, SchemaGenerator generator, String elementType) {
    var domainAssociationNode = buildDomainAssociationSchema(elementType, generator, domain);
    domainAssociationNode.put(ADDITIONAL_PROPERTIES, false);
    domainProps.set(domain.getIdAsString(), domainAssociationNode);
    return domainAssociationNode;
  }

  private ObjectNode buildDomainAssociationSchema(
      String elementType, SchemaGenerator generator, Domain domain) {
    if (elementType.equals(Control.SINGULAR_TERM)) {
      return buildDomainAssociationSchemaForControl(generator, domain);
    }
    if (elementType.equals(Scenario.SINGULAR_TERM)) {
      return buildDomainAssociationSchemaForScenario(generator, domain);
    }
    if (elementType.equals(Process.SINGULAR_TERM)) {
      return buildDomainAssociationSchemaForProcess(generator, domain);
    }
    if (elementType.equals(Scope.SINGULAR_TERM)) {
      return buildDomainAssociationSchemaForScope(generator, domain);
    }
    return generator.generateSchema(DomainAssociationDto.class);
  }

  private ObjectNode buildDomainAssociationSchemaForProcess(
      SchemaGenerator generator, Domain domain) {
    var domainAssociationSchema = generator.generateSchema(ProcessDomainAssociationDto.class);
    var riskValuesSchema = (ObjectNode) domainAssociationSchema.get(PROPS).get("riskValues");
    riskValuesSchema.put(ADDITIONAL_PROPERTIES, false);
    var riskValuesProps = riskValuesSchema.putObject(PROPS);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskDefinitionSchema = generator.generateSchema(ProcessRiskValuesDto.class);
              riskDefinitionSchema.put(ADDITIONAL_PROPERTIES, false);
              var potentialImpactsSchema =
                  (ObjectNode) riskDefinitionSchema.get(PROPS).get("potentialImpacts");

              potentialImpactsSchema.put(ADDITIONAL_PROPERTIES, false);
              var potentialImpactsSchemaProperties = potentialImpactsSchema.putObject(PROPS);
              riskDef
                  .getCategories()
                  .forEach(
                      c ->
                          potentialImpactsSchemaProperties
                              .putObject(c.getId())
                              .put(TYPE, "number")
                              .putArray("enum")
                              .addAll(
                                  c.getPotentialImpacts().stream()
                                      .map(DiscreteValue::getOrdinalValue)
                                      .map(IntNode::new)
                                      .collect(Collectors.toList())));
              riskValuesProps.set(riskDefId, riskDefinitionSchema);
            });
    return domainAssociationSchema;
  }

  private ObjectNode buildDomainAssociationSchemaForControl(
      SchemaGenerator generator, Domain domain) {
    var domainAssociationSchema = generator.generateSchema(ControlDomainAssociationDto.class);
    var riskValuesProps =
        ((ObjectNode) domainAssociationSchema.get(PROPS).get(RISK_VALUES)).putObject(PROPS);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskValuesSchema = generator.generateSchema(ControlRiskValuesDto.class);
              var implStatusSchema =
                  (ObjectNode) riskValuesSchema.get(PROPS).get("implementationStatus");
              implStatusSchema
                  .putArray("enum")
                  .addAll(
                      riskDef.getImplementationStateDefinition().getLevels().stream()
                          .map(DiscreteValue::getOrdinalValue)
                          .map(IntNode::new)
                          .collect(Collectors.toList()));
              riskValuesSchema.put(ADDITIONAL_PROPERTIES, false);
              riskValuesProps.set(riskDefId, riskValuesSchema);
            });
    return domainAssociationSchema;
  }

  private ObjectNode buildDomainAssociationSchemaForScenario(
      SchemaGenerator generator, Domain domain) {
    var domainAssociationSchema = generator.generateSchema(ScenarioDomainAssociationDto.class);
    var riskValuesProps =
        ((ObjectNode) domainAssociationSchema.get(PROPS).get(RISK_VALUES)).putObject(PROPS);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskValuesSchema = generator.generateSchema(ScenarioRiskValuesDto.class);
              var potentialProbabilitySchema =
                  (ObjectNode) riskValuesSchema.get(PROPS).get("potentialProbability");
              potentialProbabilitySchema
                  .putArray("enum")
                  .addAll(
                      riskDef.getProbability().getLevels().stream()
                          .map(DiscreteValue::getOrdinalValue)
                          .map(IntNode::new)
                          .collect(Collectors.toList()));
              riskValuesSchema.put(ADDITIONAL_PROPERTIES, false);
              riskValuesProps.set(riskDefId, riskValuesSchema);
            });
    return domainAssociationSchema;
  }

  private ObjectNode buildDomainAssociationSchemaForScope(
      SchemaGenerator generator, Domain domain) {
    var domainAssociationSchema = generator.generateSchema(ScopeDomainAssociationDto.class);
    var riskDefinitionNode = (ObjectNode) domainAssociationSchema.get(PROPS).get("riskDefinition");
    riskDefinitionNode
        .putArray("enum")
        .addAll(
            domain.getRiskDefinitions().keySet().stream()
                .map(TextNode::new)
                .collect(Collectors.toList()));
    return domainAssociationSchema;
  }

  private void addSubTypes(
      ObjectNode domainAssociationSchema, Map<String, SubTypeDefinition> subTypes) {
    // Make status required if there is a sub type and vice versa.
    var dependentRequired = domainAssociationSchema.putObject("dependentRequired");
    dependentRequired.putArray("subType").add("status");
    dependentRequired.putArray("status").add("subType");

    // Add enum with allowed sub types. Also define allowed statuses per sub type by
    // adding a list of conditions.
    var subTypeSchema = (ObjectNode) domainAssociationSchema.get(PROPS).get("subType");
    var subTypesEnum = subTypeSchema.putArray("enum");
    var conditions = domainAssociationSchema.putArray("allOf");

    // Define status as an enum value with all the possible statuses from all
    // possible subtypes.
    var commonStatusEnum =
        ((ObjectNode) domainAssociationSchema.get(PROPS).get("status")).putArray("enum");
    subTypes.values().stream()
        .flatMap(s -> s.getStatuses().stream())
        .distinct()
        .forEach(commonStatusEnum::add);

    subTypes.forEach(
        (subTypeKey, definition) -> {
          subTypesEnum.add(subTypeKey);
          var condition = conditions.addObject();
          condition.putObject("if").putObject(PROPS).putObject("subType").put("const", subTypeKey);
          var statusEnum =
              condition.putObject("then").putObject(PROPS).putObject("status").putArray("enum");
          definition.getStatuses().forEach(statusEnum::add);
        });
  }

  private void addCustomAspects(
      ObjectNode customAspectProps,
      Map<String, CustomAspectDefinition> customAspects,
      SchemaGenerator generator) {
    customAspects.forEach(
        (type, definition) ->
            customAspectProps.set(type, createCustomAspectSchema(generator, definition)));
  }

  private void addLinks(
      ObjectNode linkProps, Map<String, LinkDefinition> links, SchemaGenerator generator) {
    links.forEach(
        (type, definition) -> {
          var linkPropNode = linkProps.putObject(type);
          linkPropNode.put(TYPE, "array");
          linkPropNode.set("items", createLinkSchema(generator, definition));
        });
  }

  private ObjectNode createCustomAspectSchema(
      SchemaGenerator generator, CustomAspectDefinition definition) {
    var caSchema = generator.generateSchema(CustomAspectDto.class);
    addAttributes(caSchema, definition.getAttributeSchemas());
    return caSchema;
  }

  private ObjectNode createLinkSchema(SchemaGenerator generator, LinkDefinition definition) {
    var linkSchema = generator.generateSchema(CustomLinkDto.class);
    addAttributes(linkSchema, definition.getAttributeSchemas());

    // Add target type & sub type constraints. These constraints can't be enforced
    // by JSON schema validation and are only addded to the schema as meta
    // information for the client.
    var targetProps = (ObjectNode) linkSchema.get(PROPS).get("target").get(PROPS);
    targetProps.putObject(TYPE).putArray("enum").add(definition.getTargetType());
    Optional.ofNullable(definition.getTargetSubType())
        .ifPresent(subType -> targetProps.putObject("subType").putArray("enum").add(subType));

    return linkSchema;
  }

  private void addAttributes(ObjectNode parentSchema, Map<String, Object> attributeSchemas) {
    var attributesNode = (ObjectNode) parentSchema.get(PROPS).get("attributes");
    attributesNode.put(ADDITIONAL_PROPERTIES, false);

    var attributePropsNode = attributesNode.putObject(PROPS);
    attributeSchemas.forEach(
        (attributeKey, attributeSchema) ->
            attributePropsNode.set(attributeKey, mapper.valueToTree(attributeSchema)));
  }
}
