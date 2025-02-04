/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.adapter.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import org.veo.core.entity.ElementType;
import org.veo.core.entity.TranslationProvider;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
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
import org.veo.core.entity.transform.EntityFactory;

/**
 * This class serves as a bridge between {@link ElementTypeDefinition} and the object JSON schema
 * used by the Object Schema Editor. It can update an existing element type definition with a
 * Jackson node representing the schema.
 */
// TODO #3042: remove this when we remove support for JSON schema
@Deprecated
public class ObjectSchemaParser {

  private static final ObjectMapper OBJECTMAPPER =
      new ObjectMapper().registerModule(new BlackbirdModule());

  private static final String PROPERTIES = "properties";

  private final EntityFactory entityFactory;

  public ObjectSchemaParser(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
  }

  public ElementTypeDefinition parseTypeDefinitionFromObjectSchema(
      ElementType type, JsonNode schemaNode) throws JsonProcessingException {
    JsonNode properties = schemaNode.required(PROPERTIES);
    ElementTypeDefinition typeDefinition =
        entityFactory.createElementTypeDefinition(type.getSingularTerm(), null);
    typeDefinition.setSubTypes(extractSubTypeDefinitions(properties));
    typeDefinition.setCustomAspects(extractCustomAspectDefinitions(properties));
    typeDefinition.setLinks(extractLinkDefinitions(properties));
    typeDefinition.setTranslations(extractTranslations(properties));
    return typeDefinition;
  }

  private Map<String, SubTypeDefinition> extractSubTypeDefinitions(JsonNode properties) {
    JsonNode domains = properties.required("domains");

    JsonNode allOf = domains.required(PROPERTIES).elements().next().required("allOf");

    return StreamSupport.stream(allOf.spliterator(), false)
        .collect(
            Collectors.toMap(
                subTypeInfo ->
                    subTypeInfo
                        .required("if")
                        .required(PROPERTIES)
                        .required("subType")
                        .required("const")
                        .asText(),
                subTypeInfo -> {
                  JsonNode statusValues =
                      subTypeInfo
                          .required("then")
                          .required(PROPERTIES)
                          .required("status")
                          .required("enum");
                  SubTypeDefinition subTypeDefinition = new SubTypeDefinition();
                  subTypeDefinition.setStatuses(
                      StreamSupport.stream(statusValues.spliterator(), false)
                          .map(JsonNode::asText)
                          .toList());
                  return subTypeDefinition;
                }));
  }

  private Map<String, CustomAspectDefinition> extractCustomAspectDefinitions(JsonNode properties) {
    JsonNode customAspectsProperties = properties.required("customAspects").required(PROPERTIES);
    Iterator<Entry<String, JsonNode>> aspectIt = customAspectsProperties.fields();
    Map<String, CustomAspectDefinition> customAspects =
        new HashMap<>(customAspectsProperties.size());
    while (aspectIt.hasNext()) {
      Entry<String, JsonNode> entry = aspectIt.next();
      String aspectName = entry.getKey();
      JsonNode aspectAttributes =
          entry.getValue().required(PROPERTIES).required("attributes").required(PROPERTIES);
      CustomAspectDefinition aspectDefinition = new CustomAspectDefinition();
      Iterator<Entry<String, JsonNode>> attributeIt = aspectAttributes.fields();
      Map<String, AttributeDefinition> attributeDefinitions =
          new HashMap<>(aspectAttributes.size());
      while (attributeIt.hasNext()) {
        Entry<String, JsonNode> attributeEntry = attributeIt.next();
        attributeDefinitions.put(
            attributeEntry.getKey(), parseAttributeDefinition(attributeEntry.getValue()));
      }
      aspectDefinition.setAttributeDefinitions(attributeDefinitions);
      customAspects.put(aspectName, aspectDefinition);
    }
    return customAspects;
  }

  private AttributeDefinition parseAttributeDefinition(JsonNode jsonSchema) {
    if (jsonSchema.has("items")) {
      return new ListAttributeDefinition(parseAttributeDefinition(jsonSchema.required("items")));
    }
    if (jsonSchema.has("enum")) {
      var allowedValues = new ArrayList<String>();
      jsonSchema
          .required("enum")
          .elements()
          .forEachRemaining(n -> allowedValues.add(n.textValue()));
      return new EnumAttributeDefinition(allowedValues);
    }
    if (jsonSchema.has("format")) {
      return switch (jsonSchema.required("format").textValue()) {
        case "date" -> new DateAttributeDefinition();
        case "date-time" -> new DateTimeAttributeDefinition();
        case "uri" -> new ExternalDocumentAttributeDefinition();
        default -> throw new IllegalArgumentException("Unsupported format");
      };
    }
    if (jsonSchema.has("type")) {
      return switch (jsonSchema.required("type").textValue()) {
        case "integer" -> new IntegerAttributeDefinition();
        case "boolean" -> new BooleanAttributeDefinition();
        case "string" -> new TextAttributeDefinition();
        default -> throw new IllegalArgumentException("Unsupported type");
      };
    }
    throw new IllegalArgumentException("Unsupported attribute schema");
  }

  private Map<String, LinkDefinition> extractLinkDefinitions(JsonNode properties) {
    JsonNode linksProperties = properties.required("links").required(PROPERTIES);
    Iterator<Entry<String, JsonNode>> linkIt = linksProperties.fields();
    Map<String, LinkDefinition> links = new HashMap<>(linksProperties.size());
    while (linkIt.hasNext()) {
      Entry<String, JsonNode> entry = linkIt.next();
      String linkName = entry.getKey();
      JsonNode linkProperties = entry.getValue().required("items").required(PROPERTIES);
      LinkDefinition linkDefinition = new LinkDefinition();
      JsonNode attributeProperties = linkProperties.required("attributes").required(PROPERTIES);
      Iterator<Entry<String, JsonNode>> attributeIt = attributeProperties.fields();
      Map<String, AttributeDefinition> attributeDefinitions =
          new HashMap<>(attributeProperties.size());
      while (attributeIt.hasNext()) {
        Entry<String, JsonNode> attributeEntry = attributeIt.next();
        attributeDefinitions.put(
            attributeEntry.getKey(), parseAttributeDefinition(attributeEntry.getValue()));
      }
      linkDefinition.setAttributeDefinitions(attributeDefinitions);

      JsonNode targetProperties = linkProperties.required("target").required(PROPERTIES);

      linkDefinition.setTargetType(
          targetProperties.required("type").required("enum").required(0).asText());
      linkDefinition.setTargetSubType(
          targetProperties.required("subType").required("enum").required(0).asText());
      links.put(linkName, linkDefinition);
    }
    return links;
  }

  @SuppressWarnings("unchecked")
  private Map<Locale, Map<String, String>> extractTranslations(JsonNode properties)
      throws JsonProcessingException {
    JsonNode translationsNode = properties.required("translations");
    return TranslationProvider.convertLocales(
        OBJECTMAPPER.treeToValue(translationsNode, Map.class));
  }
}
