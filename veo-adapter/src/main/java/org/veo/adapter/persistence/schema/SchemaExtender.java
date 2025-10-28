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
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.ElementTypeDtoInfo;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.ImpactValuesDto;
import org.veo.adapter.presenter.api.dto.LinkDto;
import org.veo.adapter.presenter.api.dto.ScenarioRiskValuesDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.definitions.ControlImplementationDefinition;
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
import org.veo.core.entity.risk.ImpactMethod;
import org.veo.core.entity.risk.ImpactReason;
import org.veo.core.entity.riskdefinition.DiscreteValue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.module.blackbird.BlackbirdModule;

/** Add domain-specific sub schemas to an element schema. */
public class SchemaExtender {
  private final SchemaProvider provider = SchemaProvider.getInstance();
  private final Supplier<ObjectNode> scenarioRiskValuesDto =
      provider.schema(ScenarioRiskValuesDto.class);
  private final Supplier<ObjectNode> customAspectDto = provider.schema(CustomAspectDto.class);
  private final Supplier<ObjectNode> customLinkDto = provider.schema(CustomLinkDto.class);
  private final Supplier<ObjectNode> linkDto = provider.schema(LinkDto.class);
  private final Supplier<ObjectNode> impactValuesDto = provider.schema(ImpactValuesDto.class);
  private final Map<ElementType, Supplier<ObjectNode>> domainAssociationDtoByElementType =
      Arrays.stream(ElementTypeDtoInfo.values())
          .collect(
              Collectors.toMap(
                  ElementTypeDtoInfo::getElementType,
                  et -> provider.schema(et.getDomainAssociationDtoClass())));

  public static final String PROPS = "properties";
  public static final String RISK_VALUES = "riskValues";
  public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
  public static final String TYPE = "type";
  private static final String EXTERNAL_DOCUMENT_PATTERN =
      ExternalDocumentAttributeDefinition.PROTOCOL_PATTERN + ".+";
  private final ObjectMapper mapper = JsonMapper.builder().addModule(new BlackbirdModule()).build();
  private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

  /**
   * Extend {@link org.veo.adapter.presenter.api.dto.AbstractElementDto} schema with domain-specific
   * definitions
   */
  @Deprecated
  public void extendSchema(JsonNode schema, ElementType elementType, Set<Domain> domains) {
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
  public void extendSchema(ObjectNode schema, ElementType elementType, Domain domain) {
    var linkProps = putProps(schema, "links");
    var customAspectProps = putProps(schema, "customAspects");
    var typeDef = domain.getElementTypeDefinition(elementType);
    // TODO #3211 ban additional properties
    // schema.put(ADDITIONAL_PROPERTIES, false);
    addSubTypes(schema, typeDef.getSubTypes());
    addCustomAspectMap(customAspectProps, typeDef.getCustomAspects());
    addLinks(linkProps, typeDef.getLinks(), linkDto);
    extendDomainAssociationSchema(schema, elementType, domain);
    if (typeDef.getControlImplementationDefinition() != null) {
      var ciSchema = schema.required(PROPS).required("controlImplementations").required("items");
      extendSchema((ObjectNode) ciSchema, typeDef.getControlImplementationDefinition());
    }
  }

  public void extendSchema(
      ObjectNode ciSchema,
      @NotNull ControlImplementationDefinition controlImplementationDefinition) {
    // TODO #4420 remove this once the "customAspects" property is added to the CI
    ((ObjectNode) ciSchema.required(PROPS)).putObject("customAspects");
    var ciCustomAspectProps = putProps(ciSchema, "customAspects");
    addCustomAspectMap(ciCustomAspectProps, controlImplementationDefinition.getCustomAspects());
  }

  private static ObjectNode putProps(JsonNode schema, String node) {
    var root = (ObjectNode) schema.required(PROPS).required(node);
    root.put(ADDITIONAL_PROPERTIES, false);
    return root.putObject(PROPS);
  }

  @Deprecated
  private ObjectNode addDomainAssociation(
      ObjectNode domainProps, Domain domain, ElementType elementType) {
    var domainAssociationNode = domainAssociationDtoByElementType.get(elementType).get();
    extendDomainAssociationSchema(domainAssociationNode, elementType, domain);
    domainAssociationNode.put(ADDITIONAL_PROPERTIES, false);
    domainProps.set(domain.getIdAsString(), domainAssociationNode);
    return domainAssociationNode;
  }

  private void extendDomainAssociationSchema(
      ObjectNode target, ElementType elementType, Domain domain) {
    if (elementType == ElementType.SCENARIO) {
      extendSchemaForScenario(target, domain);
    }
    if (elementType == ElementType.PROCESS) {
      extendSchemaForProcess(target, domain);
    }
    if (elementType == ElementType.ASSET) {
      extendSchemaForAsset(target, domain);
    }
    if (elementType == ElementType.SCOPE) {
      extendSchemaForScope(target, domain);
    }
  }

  private void extendSchemaForProcess(ObjectNode target, Domain domain) {
    buildImpactSchema(domain, target);
  }

  private void extendSchemaForAsset(ObjectNode target, Domain domain) {
    buildImpactSchema(domain, target);
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
    var props = (ObjectNode) target.get(PROPS);
    // Enum definitions in JSON schema must have at least one allowed value.
    if (domain.getRiskDefinitions().isEmpty()) {
      props.remove("riskDefinition");
    } else {
      var riskDefinitionNode = (ObjectNode) props.get("riskDefinition");
      riskDefinitionNode
          .putArray("enum")
          .addAll(domain.getRiskDefinitions().keySet().stream().map(StringNode::new).toList());
    }
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
    targetProps.putObject(TYPE).putArray("enum").add(definition.getTargetType().getSingularTerm());
    targetProps.putObject("subType").putArray("enum").add(definition.getTargetSubType());
    return linkSchema;
  }

  private void buildImpactSchema(Domain domain, ObjectNode domainAssociationSchema) {
    var riskValuesProps = putProps(domainAssociationSchema, RISK_VALUES);
    domain
        .getRiskDefinitions()
        .forEach(
            (riskDefId, riskDef) -> {
              var riskDefinitionSchema = impactValuesDto.get();
              riskDefinitionSchema.put(ADDITIONAL_PROPERTIES, false);
              var potImpactsProps = putProps(riskDefinitionSchema, "potentialImpacts");
              var potImpactsCalculatedProps =
                  putProps(riskDefinitionSchema, "potentialImpactsCalculated");
              var potImpactsEffectiveProps =
                  putProps(riskDefinitionSchema, "potentialImpactsEffective");
              var potImpactReasonsProps = putProps(riskDefinitionSchema, "potentialImpactReasons");
              var potImpactEffectiveReasonsProps =
                  putProps(riskDefinitionSchema, "potentialImpactEffectiveReasons");
              var potImpactExplanationsProps =
                  putProps(riskDefinitionSchema, "potentialImpactExplanations");

              riskDef
                  .getCategories()
                  .forEach(
                      c -> {
                        var impactValueSchema = FACTORY.objectNode();
                        impactValueSchema
                            .putArray("enum")
                            .addAll(
                                c.getPotentialImpacts().stream()
                                    .map(DiscreteValue::getOrdinalValue)
                                    .map(IntNode::new)
                                    .toList());

                        potImpactsProps.set(c.getId(), impactValueSchema);
                        potImpactsCalculatedProps.set(c.getId(), impactValueSchema);
                        potImpactsEffectiveProps.set(c.getId(), impactValueSchema);

                        potImpactReasonsProps
                            .putObject(c.getId())
                            .putArray("enum")
                            .addAll(
                                Arrays.stream(ImpactReason.values())
                                    .map(ImpactReason::getTranslationKey)
                                    .map(StringNode::new)
                                    .toList());
                        potImpactEffectiveReasonsProps
                            .putObject(c.getId())
                            .putArray("enum")
                            .addAll(
                                Arrays.stream(ImpactReason.values())
                                    .map(ImpactReason::getTranslationKey)
                                    .map(StringNode::new)
                                    .toList())
                            .addAll(
                                Arrays.stream(ImpactMethod.values())
                                    .map(ImpactMethod::getTranslationKey)
                                    .map(StringNode::new)
                                    .toList());
                        potImpactExplanationsProps.putObject(c.getId()).put(TYPE, "string");
                      });
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

  private JsonNode buildAttributeJsonSchema(@NotNull AttributeDefinition attributeDefinition) {
    var schema = mapper.createObjectNode();
    switch (attributeDefinition) {
      case BooleanAttributeDefinition _ -> schema.put("type", "boolean");
      case DateAttributeDefinition _ -> {
        schema.put("type", "string");
        schema.put("format", "date");
      }
      case DateTimeAttributeDefinition _ -> {
        schema.put("type", "string");
        schema.put("format", "date-time");
      }
      case EnumAttributeDefinition enumDefinition -> {
        schema.put("type", "string");
        var allowedValues = schema.putArray("enum");
        enumDefinition.getAllowedValues().forEach(allowedValues::add);
      }
      case ExternalDocumentAttributeDefinition _ -> {
        schema.put("type", "string");
        schema.put("format", "uri");
        schema.put("pattern", EXTERNAL_DOCUMENT_PATTERN);
      }
      case IntegerAttributeDefinition _ -> schema.put("type", "integer");
      case ListAttributeDefinition listDefinition -> {
        schema.put("type", "array");
        schema.set("items", buildAttributeJsonSchema(listDefinition.getItemDefinition()));
      }
      case TextAttributeDefinition _ -> schema.put("type", "string");
    }
    return schema;
  }
}
