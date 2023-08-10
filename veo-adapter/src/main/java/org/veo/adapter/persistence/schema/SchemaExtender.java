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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.veo.adapter.presenter.api.ElementTypeDtoInfo;
import org.veo.adapter.presenter.api.dto.ControlRiskValuesDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.ImpactRiskValuesDto;
import org.veo.adapter.presenter.api.dto.LinkDto;
import org.veo.adapter.presenter.api.dto.ScenarioRiskValuesDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition;
import org.veo.core.entity.definitions.attribute.DateAttributeDefinition;
import org.veo.core.entity.definitions.attribute.DateTimeAttributeDefinition;
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition;
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition;
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition;
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition;
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition;
import org.veo.core.entity.riskdefinition.DiscreteValue;

/** Add domain-specific sub schemas to an element schema. */
public class SchemaExtender {
  private final SchemaProvider provider = SchemaProvider.getInstance();
  private final Supplier<ObjectNode> controlRiskValuesDto =
      provider.schema(ControlRiskValuesDto.class);
  private final Supplier<ObjectNode> scenarioRiskValuesDto =
      provider.schema(ScenarioRiskValuesDto.class);
  private final Supplier<ObjectNode> customAspectDto = provider.schema(CustomAspectDto.class);
  private final Supplier<ObjectNode> customLinkDto = provider.schema(CustomLinkDto.class);
  private final Supplier<ObjectNode> linkDto = provider.schema(LinkDto.class);
  private final Supplier<ObjectNode> impactRiskValuesDto =
      provider.schema(ImpactRiskValuesDto.class);
  private final Map<String, Supplier<ObjectNode>> domainAssociationDtoByElementType =
      Arrays.stream(ElementTypeDtoInfo.values())
          .collect(
              Collectors.toMap(
                  ElementTypeDtoInfo::getSingularTerm,
                  et -> provider.schema(et.getDomainAssociationDtoClass())));

  public static final String PROPS = "properties";
  public static final String RISK_VALUES = "riskValues";
  public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
  public static final String TYPE = "type";
  private static final String EXTERNAL_DOCUMENT_PATTERN =
      ExternalDocumentAttributeDefinition.PROTOCOL_PATTERN + ".+";
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Extend {@link org.veo.adapter.presenter.api.dto.AbstractElementDto} schema with domain-specific
   * definitions
   */
  @Deprecated
  public void extendSchema(JsonNode schema, String elementType, Set<Domain> domains) {
    var domainsRoot = (ObjectNode) schema.get(PROPS).get("domains");
    var domainProps = domainsRoot.putObject(PROPS);

    var linkProps = putProps(schema, "links");
    var customObjectProperties = putProps(schema, "customAspects");

    domains.forEach(
        domain -> {
          var domainAssociationSchema = addDomainAssociation(domainProps, domain, elementType);
          var typeDef = domain.getElementTypeDefinition(elementType);
          addSubTypes(domainAssociationSchema, typeDef.getSubTypes());
          addCustomAspects(customObjectProperties, typeDef.getCustomAspects());
          addLinks(linkProps, typeDef.getLinks(), customLinkDto);
        });
  }

  /**
   * Extend {@link org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto} schema with
   * domain-specific definitions
   */
  public void extendSchema(ObjectNode schema, String elementType, Domain domain) {
    var linkProps = putProps(schema, "links");
    var customAspectProps = putProps(schema, "customAspects");
    var typeDef = domain.getElementTypeDefinition(elementType);
    addSubTypes(schema, typeDef.getSubTypes());
    addCustomAspectMap(customAspectProps, typeDef.getCustomAspects());
    addLinks(linkProps, typeDef.getLinks(), linkDto);
    extendDomainAssociationSchema(schema, elementType, domain);
  }

  private static ObjectNode putProps(JsonNode schema, String node) {
    var root = (ObjectNode) schema.required(PROPS).required(node);
    root.put(ADDITIONAL_PROPERTIES, false);
    return root.putObject(PROPS);
  }

  @Deprecated
  private ObjectNode addDomainAssociation(
      ObjectNode domainProps, Domain domain, String elementType) {
    var domainAssociationNode = domainAssociationDtoByElementType.get(elementType).get();
    extendDomainAssociationSchema(domainAssociationNode, elementType, domain);
    domainAssociationNode.put(ADDITIONAL_PROPERTIES, false);
    domainProps.set(domain.getIdAsString(), domainAssociationNode);
    return domainAssociationNode;
  }

  private void extendDomainAssociationSchema(ObjectNode target, String elementType, Domain domain) {
    if (elementType.equals(Control.SINGULAR_TERM)) {
      extendSchemaForControl(target, domain);
    }
    if (elementType.equals(Scenario.SINGULAR_TERM)) {
      extendSchemaForScenario(target, domain);
    }
    if (elementType.equals(Process.SINGULAR_TERM)) {
      extendSchemaForProcess(target, domain);
    }
    if (elementType.equals(Asset.SINGULAR_TERM)) {
      extendSchemaForAsset(target, domain);
    }
    if (elementType.equals(Scope.SINGULAR_TERM)) {
      extendSchemaForScope(target, domain);
    }
  }

  private void extendSchemaForProcess(ObjectNode target, Domain domain) {
    buildImpactSchema(domain, target);
  }

  private void extendSchemaForAsset(ObjectNode target, Domain domain) {
    buildImpactSchema(domain, target);
  }

  private void extendSchemaForControl(ObjectNode target, Domain domain) {
    var riskValuesProps = ((ObjectNode) target.get(PROPS).get(RISK_VALUES)).putObject(PROPS);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskValuesSchema = controlRiskValuesDto.get();
              var implStatusSchema =
                  (ObjectNode) riskValuesSchema.get(PROPS).get("implementationStatus");
              implStatusSchema
                  .putArray("enum")
                  .addAll(
                      riskDef.getImplementationStateDefinition().getLevels().stream()
                          .map(DiscreteValue::getOrdinalValue)
                          .map(IntNode::new)
                          .toList());
              riskValuesSchema.put(ADDITIONAL_PROPERTIES, false);
              riskValuesProps.set(riskDefId, riskValuesSchema);
            });
  }

  private void extendSchemaForScenario(ObjectNode target, Domain domain) {
    var riskValuesProps = ((ObjectNode) target.get(PROPS).get(RISK_VALUES)).putObject(PROPS);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskValuesSchema = scenarioRiskValuesDto.get();
              var potentialProbabilitySchema =
                  (ObjectNode) riskValuesSchema.get(PROPS).get("potentialProbability");
              potentialProbabilitySchema
                  .putArray("enum")
                  .addAll(
                      riskDef.getProbability().getLevels().stream()
                          .map(DiscreteValue::getOrdinalValue)
                          .map(IntNode::new)
                          .toList());
              riskValuesSchema.put(ADDITIONAL_PROPERTIES, false);
              riskValuesProps.set(riskDefId, riskValuesSchema);
            });
  }

  private void extendSchemaForScope(ObjectNode target, Domain domain) {
    var riskDefinitionNode = (ObjectNode) target.get(PROPS).get("riskDefinition");
    riskDefinitionNode
        .putArray("enum")
        .addAll(domain.getRiskDefinitions().keySet().stream().map(TextNode::new).toList());
    buildImpactSchema(domain, target);
  }

  private void addSubTypes(
      ObjectNode domainAssociationSchema, Map<String, SubTypeDefinition> subTypes) {
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

  /** Fill {@link org.veo.adapter.presenter.api.dto.LinkMapDto} schema (new flat structure) */
  private void addCustomAspectMap(
      ObjectNode customAspectProps, Map<String, CustomAspectDefinition> customAspects) {
    customAspects.forEach(
        (type, definition) -> {
          var attributesNode = customAspectProps.putObject(type);
          addAttributes(attributesNode, definition.getAttributeDefinitions());
        });
  }

  /**
   * Fill {@link org.veo.adapter.presenter.api.dto.CustomAspectDto} map schema (old fat structure).
   */
  @Deprecated
  private void addCustomAspects(
      ObjectNode customAspectProps, Map<String, CustomAspectDefinition> customAspects) {
    customAspects.forEach(
        (type, definition) -> customAspectProps.set(type, createCustomAspectSchema(definition)));
  }

  private void addLinks(
      ObjectNode linkProps, Map<String, LinkDefinition> links, Supplier<ObjectNode> linkDto) {
    links.forEach(
        (type, definition) -> {
          var linkPropNode = linkProps.putObject(type);
          linkPropNode.put(TYPE, "array");
          linkPropNode.set("items", createLinkSchema(definition, linkDto));
        });
  }

  @Deprecated
  private ObjectNode createCustomAspectSchema(CustomAspectDefinition definition) {
    var caSchema = customAspectDto.get();
    addAttributes(
        (ObjectNode) caSchema.get(PROPS).get("attributes"), definition.getAttributeDefinitions());
    return caSchema;
  }

  private ObjectNode createLinkSchema(LinkDefinition definition, Supplier<ObjectNode> linkDto) {
    var linkSchema = linkDto.get();
    addAttributes(
        (ObjectNode) linkSchema.get(PROPS).get("attributes"), definition.getAttributeDefinitions());

    // Add target type & sub type constraints. These constraints can't be enforced
    // by JSON schema validation and are only addded to the schema as meta
    // information for the client.
    var targetProps = (ObjectNode) linkSchema.get(PROPS).get("target").get(PROPS);
    targetProps.putObject(TYPE).putArray("enum").add(definition.getTargetType());
    Optional.ofNullable(definition.getTargetSubType())
        .ifPresent(subType -> targetProps.putObject("subType").putArray("enum").add(subType));

    return linkSchema;
  }

  private void buildImpactSchema(Domain domain, ObjectNode domainAssociationSchema) {
    var riskValuesProps = putProps(domainAssociationSchema, RISK_VALUES);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskDefinitionSchema = impactRiskValuesDto.get();
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
                                      .toList()));
              riskValuesProps.set(riskDefId, riskDefinitionSchema);
            });
  }

  private void addAttributes(
      ObjectNode attributesNode, Map<String, AttributeDefinition> attributeDefinitions) {
    attributesNode.put(ADDITIONAL_PROPERTIES, false);

    var attributePropsNode = attributesNode.putObject(PROPS);
    attributeDefinitions.forEach(
        (attributeKey, definition) -> {
          attributePropsNode.set(attributeKey, buildAttributeJsonSchema(definition));
        });
  }

  private JsonNode buildAttributeJsonSchema(AttributeDefinition attributeDefinition) {
    var schema = mapper.createObjectNode();
    if (attributeDefinition instanceof BooleanAttributeDefinition) {
      schema.put("type", "boolean");
    } else if (attributeDefinition instanceof DateAttributeDefinition) {
      schema.put("type", "string");
      schema.put("format", "date");
    } else if (attributeDefinition instanceof DateTimeAttributeDefinition) {
      schema.put("type", "string");
      schema.put("format", "date-time");
    } else if (attributeDefinition instanceof EnumAttributeDefinition enumDefinition) {
      schema.put("type", "string");
      var allowedValues = schema.putArray("enum");
      enumDefinition.getAllowedValues().forEach(allowedValues::add);
    } else if (attributeDefinition instanceof ExternalDocumentAttributeDefinition) {
      schema.put("type", "string");
      schema.put("format", "uri");
      schema.put("pattern", EXTERNAL_DOCUMENT_PATTERN);
    } else if (attributeDefinition instanceof IntegerAttributeDefinition) {
      schema.put("type", "integer");
    } else if (attributeDefinition instanceof ListAttributeDefinition listDefinition) {
      schema.put("type", "array");
      schema.set("items", buildAttributeJsonSchema(listDefinition.getItemDefinition()));
    } else if (attributeDefinition instanceof TextAttributeDefinition) {
      schema.put("type", "string");
    } else
      throw new NotImplementedException(
          "JSON schema creation not implemented for attribute type %s"
              .formatted(attributeDefinition.getClass().getSimpleName()));
    return schema;
  }
}
