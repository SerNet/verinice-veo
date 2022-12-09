/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.rest.common.marshalling;

import static java.lang.String.format;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.ANY_BOOLEAN;
import static org.veo.rest.ControllerConstants.ANY_INT;
import static org.veo.rest.ControllerConstants.ANY_REQUEST;
import static org.veo.rest.ControllerConstants.ANY_SEARCH;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.ANY_STRING_LIST;
import static org.veo.rest.ControllerConstants.ANY_USER;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.ModelDto;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CatalogReference;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.Unit;
import org.veo.rest.AssetController;
import org.veo.rest.AssetRiskResource;
import org.veo.rest.CatalogController;
import org.veo.rest.ControlController;
import org.veo.rest.DocumentController;
import org.veo.rest.DomainController;
import org.veo.rest.DomainTemplateController;
import org.veo.rest.IncidentController;
import org.veo.rest.PersonController;
import org.veo.rest.ProcessController;
import org.veo.rest.ProcessRiskResource;
import org.veo.rest.ScenarioController;
import org.veo.rest.ScopeController;
import org.veo.rest.ScopeRiskResource;
import org.veo.rest.UnitController;
import org.veo.rest.configuration.TypeExtractor;
import org.veo.rest.schemas.controller.EntitySchemaController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class ReferenceAssemblerImpl implements ReferenceAssembler {

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

  private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

  private final TypeExtractor typeExtractor;

  @Override
  @SuppressFBWarnings // ignore warning on call to method proxy factory
  public String targetReferenceOf(Identifiable identifiable) {
    Class<? extends Identifiable> type = identifiable.getModelInterface();
    String id = identifiable.getId().uuidValue();
    if (Scope.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ScopeController.class).getScope(ANY_AUTH, id, ANY_REQUEST))
          .withRel(ScopeController.URL_BASE_PATH)
          .getHref();
    }
    if (Asset.class.isAssignableFrom(type)) {
      return linkTo(methodOn(AssetController.class).getElement(ANY_AUTH, id, ANY_REQUEST))
          .withRel(AssetController.URL_BASE_PATH)
          .getHref();
    }
    if (Document.class.isAssignableFrom(type)) {
      return linkTo(methodOn(DocumentController.class).getElement(ANY_AUTH, id, ANY_REQUEST))
          .withRel(DocumentController.URL_BASE_PATH)
          .getHref();
    }
    if (Unit.class.isAssignableFrom(type)) {
      return linkTo(methodOn(UnitController.class).getUnit(ANY_AUTH, id, ANY_REQUEST))
          .withRel(UnitController.URL_BASE_PATH)
          .getHref();
    }
    if (Person.class.isAssignableFrom(type)) {
      return linkTo(methodOn(PersonController.class).getElement(ANY_AUTH, id, ANY_REQUEST))
          .withRel(PersonController.URL_BASE_PATH)
          .getHref();
    }
    if (Process.class.isAssignableFrom(type)) {
      return trimVariables(
          linkTo(
                  methodOn(ProcessController.class)
                      .getProcess(ANY_AUTH, id, ANY_BOOLEAN, ANY_REQUEST))
              .withRel(ProcessController.URL_BASE_PATH)
              .getHref());
    }
    if (Control.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ControlController.class).getElement(ANY_AUTH, id, ANY_REQUEST))
          .withRel(ControlController.URL_BASE_PATH)
          .getHref();
    }
    if (Scenario.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ScenarioController.class).getElement(ANY_AUTH, id, ANY_REQUEST))
          .withRel(ScenarioController.URL_BASE_PATH)
          .getHref();
    }
    if (Incident.class.isAssignableFrom(type)) {
      return linkTo(methodOn(IncidentController.class).getElement(ANY_AUTH, id, ANY_REQUEST))
          .withRel(IncidentController.URL_BASE_PATH)
          .getHref();
    }
    if (Domain.class.isAssignableFrom(type)) {
      return linkTo(methodOn(DomainController.class).getDomain(ANY_AUTH, id, ANY_REQUEST))
          .withRel(DomainController.URL_BASE_PATH)
          .getHref();
    }
    if (DomainTemplate.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(DomainTemplateController.class).getDomainTemplate(ANY_AUTH, id, ANY_REQUEST))
          .withRel(DomainTemplateController.URL_BASE_PATH)
          .getHref();
    }
    if (Catalog.class.isAssignableFrom(type)) {
      return linkTo(methodOn(CatalogController.class).getCatalog(ANY_AUTH, id, ANY_REQUEST))
          .withRel(CatalogController.URL_BASE_PATH)
          .getHref();
    }
    if (CatalogItem.class.isAssignableFrom(type)) {
      CatalogItem catalogItem = (CatalogItem) identifiable;
      return linkTo(
              methodOn(CatalogController.class)
                  .getCatalogItem(
                      ANY_AUTH,
                      catalogItem.getCatalog().getId().uuidValue(),
                      id,
                      null,
                      ANY_REQUEST))
          .withRel(CatalogController.URL_BASE_PATH)
          .expand()
          .getHref();
    }
    // Some types have no endpoint.
    if (Client.class.isAssignableFrom(type) || CatalogReference.class.isAssignableFrom(type)) {
      return null;
    }

    throw new NotImplementedException("Unsupported reference type " + type);
  }

  /**
   * HATEOAS links may contain a list of optional variables that are invalid as a URI if they are
   * not expanded with values (i.e. "{@code {?embedRisks=false}}"). This method removes those
   * because we have many places that do not expect optional variables in the reference URI and trip
   * over them.
   */
  // TODO VEO-1352 remove this method when users can handle the URI template
  // format
  private String trimVariables(String href) {
    if (href.contains("{")) return href.split("\\{")[0];
    return href;
  }

  @Override
  @SuppressFBWarnings // ignore warning on call to method proxy factory
  public String targetReferenceOf(AbstractRisk<?, ?> risk) {
    var entityId = risk.getEntity().getId().uuidValue();
    var scenarioId = risk.getScenario().getId().uuidValue();
    if (risk instanceof AssetRisk) {
      return linkTo(methodOn(AssetController.class).getRisk(ANY_USER, entityId, scenarioId))
          .withRel(AssetController.URL_BASE_PATH + AssetRiskResource.RELPATH)
          .getHref();
    }
    if (risk instanceof ProcessRisk) {
      return linkTo(methodOn(ProcessController.class).getRisk(ANY_USER, entityId, scenarioId))
          .withRel(ProcessController.URL_BASE_PATH + ProcessRiskResource.RELPATH)
          .getHref();
    }
    if (risk instanceof ScopeRisk) {
      return linkTo(methodOn(ScopeController.class).getRisk(ANY_USER, entityId, scenarioId))
          .withRel(ScopeController.URL_BASE_PATH + ScopeRiskResource.RELPATH)
          .getHref();
    }
    throw new NotImplementedException(
        format("Cannot create risk reference to entity " + "%s.", risk.getClass()));
  }

  @Override
  public String searchesReferenceOf(Class<? extends Identifiable> type) {
    if (Scope.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ScopeController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(ScopeController.URL_BASE_PATH)
          .getHref();
    }
    if (Asset.class.isAssignableFrom(type)) {
      return linkTo(methodOn(AssetController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(AssetController.URL_BASE_PATH)
          .getHref();
    }
    if (Document.class.isAssignableFrom(type)) {
      return linkTo(methodOn(DocumentController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(AssetController.URL_BASE_PATH)
          .getHref();
    }
    if (Unit.class.isAssignableFrom(type)) {
      return linkTo(methodOn(UnitController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(UnitController.URL_BASE_PATH)
          .getHref();
    }
    if (Process.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ProcessController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(ProcessController.URL_BASE_PATH)
          .getHref();
    }
    if (Person.class.isAssignableFrom(type)) {
      return linkTo(methodOn(PersonController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(PersonController.URL_BASE_PATH)
          .getHref();
    }
    if (Control.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ControlController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(ControlController.URL_BASE_PATH)
          .getHref();
    }
    if (Scenario.class.isAssignableFrom(type)) {
      return linkTo(methodOn(ScenarioController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(ScenarioController.URL_BASE_PATH)
          .getHref();
    }
    if (Incident.class.isAssignableFrom(type)) {
      return linkTo(methodOn(IncidentController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(IncidentController.URL_BASE_PATH)
          .getHref();
    }
    if (Domain.class.isAssignableFrom(type)) {
      return linkTo(methodOn(DomainController.class).createSearch(ANY_AUTH, ANY_SEARCH))
          .withRel(DomainController.URL_BASE_PATH)
          .getHref();
    }
    // Some types have no endpoint.
    if (Client.class.isAssignableFrom(type)
        || Catalog.class.isAssignableFrom(type)
        || CatalogItem.class.isAssignableFrom(type)
        || DomainTemplate.class.isAssignableFrom(type)) {
      return null;
    }
    throw new NotImplementedException("Unsupported search reference type " + type.getSimpleName());
  }

  @Override
  @SuppressFBWarnings // ignore warnings on calls to method proxy factories
  public String resourcesReferenceOf(Class<? extends Identifiable> type) {
    if (Scope.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ScopeController.class)
                  .getScopes(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Asset.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(AssetController.class)
                  .getAssets(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Document.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(DocumentController.class)
                  .getDocuments(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Unit.class.isAssignableFrom(type)) {
      return linkTo(methodOn(UnitController.class).getUnits(ANY_AUTH, ANY_STRING, ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Process.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ProcessController.class)
                  .getProcesses(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_BOOLEAN))
          .withSelfRel()
          .getHref();
    }
    if (Person.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(PersonController.class)
                  .getPersons(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Control.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ControlController.class)
                  .getControls(
                      ANY_AUTH, ANY_STRING,
                      ANY_STRING, ANY_STRING,
                      ANY_STRING, ANY_STRING_LIST,
                      ANY_BOOLEAN, ANY_BOOLEAN,
                      ANY_STRING, ANY_STRING,
                      ANY_STRING, ANY_STRING,
                      ANY_INT, ANY_INT,
                      ANY_STRING, ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Scenario.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ScenarioController.class)
                  .getScenarios(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Incident.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(IncidentController.class)
                  .getIncidents(
                      ANY_AUTH,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_INT,
                      ANY_INT,
                      ANY_STRING,
                      ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Domain.class.isAssignableFrom(type)) {
      return linkTo(methodOn(DomainController.class).getDomains(ANY_AUTH)).withSelfRel().getHref();
    }
    if (DomainTemplate.class.isAssignableFrom(type)) {
      return linkTo(methodOn(DomainTemplateController.class).getDomainTemplates())
          .withSelfRel()
          .getHref();
    }
    // Some types have no endpoint.
    if (Client.class.isAssignableFrom(type)
        || Catalog.class.isAssignableFrom(type)
        || CatalogItem.class.isAssignableFrom(type)) {
      return null;
    }
    throw new NotImplementedException("Unsupported collection reference type " + type);
  }

  /**
   * Compares the given URI with all mapped request methods of type "GET". Extracts the DTO type
   * used in the methods return value. Then returns the corresponding entity type.
   *
   * @param uriString the URI string received as a reference, i.e. via JSON representation of an
   *     entity
   * @return the class of the entity that is mapped by the DTO
   */
  @Override
  public Class<? extends Identifiable> parseType(String uriString) {
    Class<? extends ModelDto> modelType =
        typeExtractor
            .parseDtoType(uriString)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format("Could not extract entity type from URI: %s", uriString)));
    try {
      return modelType.getDeclaredConstructor().newInstance().getModelInterface();
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format("Could not extract entity type from URI: %s", uriString));
    }
  }

  @Override
  public String parseId(String uriString) {
    String pathComponent = UriComponentsBuilder.fromUriString(uriString).build().getPath();
    if (pathComponent == null) {
      return null;
    }
    Matcher matcher = UUID_PATTERN.matcher(pathComponent);
    if (!matcher.find()) {
      return null;
    }
    String result = matcher.group(0);
    if (matcher.find()) {
      // TODO: VEO-585: probably throw an exception
      result = matcher.group(0);
    }
    return result;
  }

  @Override
  public Key<UUID> toKey(IdRef<? extends Identifiable> reference) {
    if (reference == null) return null;
    return Key.uuidFrom(reference.getId());
  }

  @Override
  public Set<Key<UUID>> toKeys(Set<? extends IdRef<?>> references) {
    return references.stream().map(this::toKey).collect(Collectors.toSet());
  }

  @Override
  public String schemaReferenceOf(String typeSingularTerm) {
    return linkTo(
            methodOn(EntitySchemaController.class)
                .getSchema(ANY_AUTH, typeSingularTerm, ANY_STRING_LIST))
        .withSelfRel()
        .getHref();
  }
}
