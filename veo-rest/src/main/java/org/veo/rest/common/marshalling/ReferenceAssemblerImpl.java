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
import static org.veo.rest.ControllerConstants.ANY_LONG;
import static org.veo.rest.ControllerConstants.ANY_REQUEST;
import static org.veo.rest.ControllerConstants.ANY_SEARCH;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.ANY_USER;
import static org.veo.rest.ControllerConstants.ANY_UUID;
import static org.veo.rest.ControllerConstants.ANY_UUID_LIST;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Entity;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.TemplateItemReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.rest.AssetController;
import org.veo.rest.AssetInDomainController;
import org.veo.rest.AssetRiskResource;
import org.veo.rest.ContentCreationController;
import org.veo.rest.ControlController;
import org.veo.rest.ControlInDomainController;
import org.veo.rest.DocumentController;
import org.veo.rest.DocumentInDomainController;
import org.veo.rest.DomainController;
import org.veo.rest.DomainTemplateController;
import org.veo.rest.IncidentController;
import org.veo.rest.IncidentInDomainController;
import org.veo.rest.MessageController;
import org.veo.rest.PersonController;
import org.veo.rest.PersonInDomainController;
import org.veo.rest.ProcessController;
import org.veo.rest.ProcessInDomainController;
import org.veo.rest.ProcessRiskResource;
import org.veo.rest.RiskAffectedResource;
import org.veo.rest.ScenarioController;
import org.veo.rest.ScenarioInDomainController;
import org.veo.rest.ScopeController;
import org.veo.rest.ScopeInDomainController;
import org.veo.rest.ScopeRiskResource;
import org.veo.rest.UnitController;
import org.veo.rest.UserConfigurationController;
import org.veo.rest.schemas.controller.EntitySchemaController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@SuppressFBWarnings(
    value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS",
    justification = "The controller method invocations are just dummies")
public class ReferenceAssemblerImpl implements ReferenceAssembler {
  private static final String DUMMY_UUID_STRING = "00000000-0000-0000-0000-000000000000";
  private static final UUID DUMMY_UUID = UUID.fromString(DUMMY_UUID_STRING);

  private static final UriComponents GET_ASSET =
      createTemplate(
          linkTo(
                  methodOn(AssetController.class)
                      .getAsset(ANY_AUTH, DUMMY_UUID, ANY_BOOLEAN, ANY_REQUEST))
              .withRel(AssetController.URL_BASE_PATH));

  private static final UriComponents GET_CONTROL =
      createTemplate(
          linkTo(methodOn(ControlController.class).getElement(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(ControlController.URL_BASE_PATH));

  private static final UriComponents GET_DOCUMENT =
      createTemplate(
          linkTo(methodOn(DocumentController.class).getElement(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(DocumentController.URL_BASE_PATH));
  private static final UriComponents GET_DOMAIN =
      createTemplate(
          linkTo(methodOn(DomainController.class).getDomain(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(DomainController.URL_BASE_PATH));

  private static final UriComponents GET_DOMAIN_TEMPLATE =
      createTemplate(
          linkTo(methodOn(ContentCreationController.class).getDomainTemplate(ANY_AUTH, DUMMY_UUID))
              .withRel(DomainTemplateController.URL_BASE_PATH));

  private static final UriComponents GET_INCIDENT =
      createTemplate(
          linkTo(methodOn(IncidentController.class).getElement(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(IncidentController.URL_BASE_PATH));

  private static final UriComponents GET_PROFILE =
      createTemplate(
          linkTo(
                  methodOn(DomainController.class)
                      .getProfile(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(DomainController.URL_BASE_PATH));

  private static final UriComponents GET_PERSON =
      createTemplate(
          linkTo(methodOn(PersonController.class).getElement(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(PersonController.URL_BASE_PATH));

  private static final UriComponents GET_PROCESS =
      createTemplate(
          linkTo(
                  methodOn(ProcessController.class)
                      .getProcess(ANY_AUTH, DUMMY_UUID, ANY_BOOLEAN, ANY_REQUEST))
              .withRel(ProcessController.URL_BASE_PATH));

  private static final UriComponents GET_SCENARIO =
      createTemplate(
          linkTo(methodOn(ScenarioController.class).getElement(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(ScenarioController.URL_BASE_PATH));

  private static final UriComponents GET_SCOPE =
      createTemplate(
          linkTo(
                  methodOn(ScopeController.class)
                      .getScope(ANY_AUTH, DUMMY_UUID, ANY_BOOLEAN, ANY_REQUEST))
              .withRel(ScopeController.URL_BASE_PATH));

  private static final UriComponents GET_UNIT =
      createTemplate(
          linkTo(methodOn(UnitController.class).getUnit(ANY_AUTH, DUMMY_UUID, ANY_REQUEST))
              .withRel(UnitController.URL_BASE_PATH));

  private static final UriComponents GET_ASSET_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(AssetInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(AssetInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_CONTROL_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(ControlInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(ControlInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_DOCUMENT_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(DocumentInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(DocumentInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_INCIDENT_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(IncidentInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(IncidentInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_PERSON_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(PersonInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(PersonInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_PROCESS_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(ProcessInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(ProcessInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_SCENARIO_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(ScenarioInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(ScenarioInDomainController.URL_BASE_PATH));
  private static final UriComponents GET_SCOPE_IN_DOMAIN =
      createTemplate(
          linkTo(
                  methodOn(ScopeInDomainController.class)
                      .getElement(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(ScopeInDomainController.URL_BASE_PATH));

  private static final UriComponents GET_ASSET_RI =
      createTemplate(
          linkToRequirementImplementation(AssetController.class)
              .withRel(AssetController.URL_BASE_PATH));
  private static final UriComponents GET_PROCESS_RI =
      createTemplate(
          linkToRequirementImplementation(ProcessController.class)
              .withRel(ProcessController.URL_BASE_PATH));
  private static final UriComponents GET_SCOPE_RI =
      createTemplate(
          linkToRequirementImplementation(ScopeController.class)
              .withRel(ScopeController.URL_BASE_PATH));

  private static final UriComponents GET_ASSET_RIS =
      createTemplate(
          linkToRequirementImplementations(AssetController.class)
              .withRel(AssetController.URL_BASE_PATH));
  private static final UriComponents GET_PROCESS_RIS =
      createTemplate(
          linkToRequirementImplementations(ProcessController.class)
              .withRel(ProcessController.URL_BASE_PATH));
  private static final UriComponents GET_SCOPE_RIS =
      createTemplate(
          linkToRequirementImplementations(ScopeController.class)
              .withRel(ScopeController.URL_BASE_PATH));

  private static final UriComponents GET_INSPECTION =
      createTemplate(
          linkTo(
                  methodOn(DomainController.class)
                      .getInspection(ANY_AUTH, DUMMY_UUID, DUMMY_UUID_STRING))
              .withRel(DomainController.URL_BASE_PATH));

  private static final UriComponents GET_CATALOG_ITEM =
      createTemplate(
          linkTo(
                  methodOn(DomainController.class)
                      .getCatalogItem(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(DomainController.URL_BASE_PATH));

  private static final UriComponents GET_PROFILE_ITEM =
      createTemplate(
          linkTo(
                  methodOn(DomainController.class)
                      .getProfileItem(ANY_AUTH, DUMMY_UUID, DUMMY_UUID, DUMMY_UUID, ANY_REQUEST))
              .withRel(DomainController.URL_BASE_PATH));

  private static final UriComponents GET_ASSET_RISK =
      createTemplate(
          linkTo(methodOn(AssetController.class).getRisk(ANY_USER, DUMMY_UUID, DUMMY_UUID))
              .withRel(AssetController.URL_BASE_PATH + AssetRiskResource.RELPATH));

  private static final UriComponents GET_PROCESS_RISK =
      createTemplate(
          linkTo(methodOn(ProcessController.class).getRisk(ANY_USER, DUMMY_UUID, DUMMY_UUID))
              .withRel(ProcessController.URL_BASE_PATH + ProcessRiskResource.RELPATH));

  private static final UriComponents GET_SCOPE_RISK =
      createTemplate(
          linkTo(methodOn(ScopeController.class).getRisk(ANY_USER, DUMMY_UUID, DUMMY_UUID))
              .withRel(ScopeController.URL_BASE_PATH + ScopeRiskResource.RELPATH));

  private static final UriComponents GET_USER_CONFIGURATION =
      createTemplate(
          linkTo(
                  methodOn(UserConfigurationController.class)
                      .getUserConfiguration(ANY_USER, ANY_STRING))
              .withRel(UserConfigurationController.URL_BASE_PATH));

  private static final UriComponents GET_SYSTEM_MESSAGE =
      createTemplate(
          linkTo(methodOn(MessageController.class).getSystemMessage(ANY_LONG))
              .withRel(UserConfigurationController.URL_BASE_PATH));

  @Override
  public String targetReferenceOf(Identifiable identifiable) {
    Class<? extends Identifiable> type = identifiable.getModelInterface();
    UUID id = identifiable.getId();
    if (Scope.class.isAssignableFrom(type)) {
      return buildUri(GET_SCOPE, id);
    }
    if (Asset.class.isAssignableFrom(type)) {
      return buildUri(GET_ASSET, id);
    }
    if (Document.class.isAssignableFrom(type)) {
      return buildUri(GET_DOCUMENT, id);
    }
    if (Unit.class.isAssignableFrom(type)) {
      return buildUri(GET_UNIT, id);
    }
    if (Person.class.isAssignableFrom(type)) {
      return buildUri(GET_PERSON, id);
    }
    if (Process.class.isAssignableFrom(type)) {
      return buildUri(GET_PROCESS, id);
    }
    if (Control.class.isAssignableFrom(type)) {
      return buildUri(GET_CONTROL, id);
    }
    if (Scenario.class.isAssignableFrom(type)) {
      return buildUri(GET_SCENARIO, id);
    }
    if (Incident.class.isAssignableFrom(type)) {
      return buildUri(GET_INCIDENT, id);
    }
    if (Domain.class.isAssignableFrom(type)) {
      return buildUri(GET_DOMAIN, id);
    }
    if (DomainTemplate.class.isAssignableFrom(type)) {
      return buildUri(GET_DOMAIN_TEMPLATE, id);
    }
    if (Profile.class.isAssignableFrom(type)) {
      Profile profile = (Profile) identifiable;
      // TODO #2497 introduce endpoint for profiles in domain
      // templates
      return buildUri(GET_PROFILE, profile.getOwner().getId(), id);
    }
    // Some types have no endpoint.
    if (Client.class.isAssignableFrom(type) || TemplateItemReference.class.isAssignableFrom(type)) {
      return null;
    }

    throw new NotImplementedException("Unsupported reference type " + type);
  }

  @Override
  public String elementInDomainRefOf(Element element, Domain domain) {
    var type = element.getModelInterface();
    if (Asset.class.isAssignableFrom(type)) {
      return buildUri(GET_ASSET_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Control.class.isAssignableFrom(type)) {
      return buildUri(GET_CONTROL_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Document.class.isAssignableFrom(type)) {
      return buildUri(GET_DOCUMENT_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Incident.class.isAssignableFrom(type)) {
      return buildUri(GET_INCIDENT_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Person.class.isAssignableFrom(type)) {
      return buildUri(GET_PERSON_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Process.class.isAssignableFrom(type)) {
      return buildUri(GET_PROCESS_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Scenario.class.isAssignableFrom(type)) {
      return buildUri(GET_SCENARIO_IN_DOMAIN, domain.getId(), element.getId());
    }
    if (Scope.class.isAssignableFrom(type)) {
      return buildUri(GET_SCOPE_IN_DOMAIN, domain.getId(), element.getId());
    }
    throw new NotImplementedException(
        "%s references in domain context not supported".formatted(type.getSimpleName()));
  }

  @Override
  public String targetReferenceOf(RequirementImplementation requirementImplementation) {
    UUID originId = requirementImplementation.getOrigin().getId();
    UUID controlId = requirementImplementation.getControl().getId();
    return switch (requirementImplementation.getOrigin().getModelType()) {
      case Asset.SINGULAR_TERM -> buildUri(GET_ASSET_RI, originId, controlId);
      case Process.SINGULAR_TERM -> buildUri(GET_PROCESS_RI, originId, controlId);
      case Scope.SINGULAR_TERM -> buildUri(GET_SCOPE_RI, originId, controlId);
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public String requirementImplementationsOf(ControlImplementation controlImplementation) {
    UUID ownerId = controlImplementation.getOwner().getId();
    UUID controlId = controlImplementation.getControl().getId();
    return switch (controlImplementation.getOwner().getModelType()) {
      case Asset.SINGULAR_TERM -> buildUri(GET_ASSET_RIS, ownerId, controlId);
      case Process.SINGULAR_TERM -> buildUri(GET_PROCESS_RIS, ownerId, controlId);
      case Scope.SINGULAR_TERM -> buildUri(GET_SCOPE_RIS, ownerId, controlId);
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public String inspectionReferenceOf(String id, Domain domain) {
    return buildUri(GET_INSPECTION, domain.getId(), id);
  }

  @Override
  public <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      String targetReferenceOf(T entity) {
    if (entity instanceof CatalogItem catalogItem) {

      return buildUri(
          GET_CATALOG_ITEM,
          catalogItem.getDomainBase().getId(),
          catalogItem.getSymbolicIdAsString());
    }
    if (entity instanceof ProfileItem profileItem) {
      // TODO #2497 introduce endpoint for profile items in domain
      // templates
      return buildUri(
          GET_PROFILE_ITEM,
          profileItem.getDomainBase().getId(),
          profileItem.getOwner().getId(),
          profileItem.getSymbolicIdAsString());
    }
    throw new NotImplementedException();
  }

  @Override
  public String targetReferenceOf(UserConfiguration userConfiguration) {
    return buildUri(GET_USER_CONFIGURATION, userConfiguration.getApplicationId());
  }

  @Override
  public String targetReferenceOf(SystemMessage systemMessage) {
    return buildUri(GET_SYSTEM_MESSAGE, systemMessage.getId());
  }

  private static WebMvcLinkBuilder linkToRequirementImplementation(
      Class<? extends RiskAffectedResource> controller) {
    return linkTo(
        methodOn(controller).getRequirementImplementation(ANY_AUTH, DUMMY_UUID, DUMMY_UUID));
  }

  private static WebMvcLinkBuilder linkToRequirementImplementations(
      Class<? extends RiskAffectedResource> controller) {
    return linkTo(
        methodOn(controller)
            .getRequirementImplementations(
                ANY_AUTH, DUMMY_UUID, DUMMY_UUID, ANY_INT, ANY_INT, ANY_STRING, ANY_STRING));
  }

  /**
   * HATEOAS links may contain a list of optional variables that are invalid as a URI if they are
   * not expanded with values (i.e. "{@code {?embedRisks=false}}"). This method removes those
   * because we have many places that do not expect optional variables in the reference URI and trip
   * over them.
   */
  // TODO VEO-1352 remove this method when users can handle the URI template
  // format
  private static String trimVariables(String href) {
    if (href.contains("{")) return href.split("\\{")[0];
    return href;
  }

  @Override
  public String targetReferenceOf(CompoundIdentifiable<?, ?> entity) {
    var firstId = entity.getFirstIdAsString();
    var secondId = entity.getSecondIdAsString();
    if (entity instanceof AssetRisk) {
      return buildUri(GET_ASSET_RISK, firstId, secondId);
    }
    if (entity instanceof ProcessRisk) {
      return buildUri(GET_PROCESS_RISK, firstId, secondId);
    }
    if (entity instanceof ScopeRisk) {
      return buildUri(GET_SCOPE_RISK, firstId, secondId);
    }
    throw new NotImplementedException(
        format("Cannot create risk reference to entity " + "%s.", entity.getClass()));
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
        || CatalogItem.class.isAssignableFrom(type)
        || DomainTemplate.class.isAssignableFrom(type)
        || Profile.class.isAssignableFrom(type)
        || ProfileItem.class.isAssignableFrom(type)
        || AbstractRisk.class.isAssignableFrom(type)
        || UserConfiguration.class.isAssignableFrom(type)) {
      return null;
    }
    throw new NotImplementedException("Unsupported search reference type " + type.getSimpleName());
  }

  @Override
  public String resourcesReferenceOf(Class<? extends Identifiable> type) {
    if (Scope.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ScopeController.class)
                  .getScopes(
                      ANY_AUTH,
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
    if (Asset.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(AssetController.class)
                  .getAssets(
                      ANY_AUTH,
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
    if (Document.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(DocumentController.class)
                  .getDocuments(
                      ANY_AUTH,
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
      return linkTo(methodOn(UnitController.class).getUnits(ANY_AUTH, ANY_UUID, ANY_STRING))
          .withSelfRel()
          .getHref();
    }
    if (Process.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ProcessController.class)
                  .getProcesses(
                      ANY_AUTH,
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
                      ANY_AUTH,
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
    if (Scenario.class.isAssignableFrom(type)) {
      return linkTo(
              methodOn(ScenarioController.class)
                  .getScenarios(
                      ANY_AUTH,
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
                      ANY_UUID,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_STRING,
                      ANY_UUID_LIST,
                      ANY_BOOLEAN,
                      ANY_BOOLEAN,
                      ANY_STRING,
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
    if (UserConfiguration.class.isAssignableFrom(type)) {
      return linkTo(methodOn(UserConfigurationController.class).getUserConfiguration(ANY_USER))
          .withSelfRel()
          .expand()
          .getHref();
    }
    // Some types have no endpoint.
    if (Client.class.isAssignableFrom(type)
        || CatalogItem.class.isAssignableFrom(type)
        || Profile.class.isAssignableFrom(type)
        || ProfileItem.class.isAssignableFrom(type)
        || AbstractRisk.class.isAssignableFrom(type)) {
      return null;
    }
    throw new NotImplementedException("Unsupported collection reference type " + type);
  }

  @Override
  public TypedId<?> parseIdentifiableRef(String uri) {
    return parseIdentifiableRef(uri, Identifiable.class);
  }

  @Override
  public TypedId<? extends Element> parseElementRef(String url) {
    return parseIdentifiableRef(url, Element.class);
  }

  private <T extends Identifiable> TypedId<T> parseIdentifiableRef(String uri, Class<T> superType) {
    try {
      var segments = UriComponentsBuilder.fromUriString(uri).build().getPathSegments();
      var size = segments.size();
      var idSeg = segments.get(size - 1);
      var typeSeg = segments.get(size - 2);
      if (typeSeg.equals(Client.PLURAL_TERM)) {
        throw invalidReference(uri);
      }
      return TypedId.from(UUID.fromString(idSeg), parseType(typeSeg, superType));
    } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
      throw invalidReference(uri);
    }
  }

  @Override
  public ITypedSymbolicId<?, ?> parseSymIdentifiableUri(String uri) {
    try {
      var segments = UriComponentsBuilder.fromUriString(uri).build().getPathSegments();
      var size = segments.size();
      var namespaceTypeSeg = segments.get(size - 4);
      var namespaceIdSeg = segments.get(size - 3);
      var typeSeg = segments.get(size - 2);
      var symIdSeg = UUID.fromString(segments.get(size - 1));
      if (typeSeg.equals(CatalogItem.PLURAL_TERM)) {
        return TypedSymbolicId.from(
            symIdSeg,
            CatalogItem.class,
            TypedId.from(
                UUID.fromString(namespaceIdSeg), parseType(namespaceTypeSeg, DomainBase.class)));
      }
      if (namespaceTypeSeg.equals(Profile.PLURAL_TERM) && typeSeg.equals("items")) {
        return TypedSymbolicId.from(
            symIdSeg,
            ProfileItem.class,
            TypedId.from(UUID.fromString(namespaceIdSeg), Profile.class));
      }
      throw new IllegalArgumentException();
    } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
      throw invalidReference(uri);
    }
  }

  private <T extends Entity> Class<T> parseType(String pathSeg, Class<T> superType) {
    var type = EntityType.getTypeForPluralTerm(pathSeg);
    if (superType.isAssignableFrom(type)) {
      return (Class<T>) type;
    }
    throw new IllegalArgumentException();
  }

  @Override
  public UUID toKey(ITypedId<? extends Identifiable> reference) {
    if (reference == null) return null;
    return reference.getId();
  }

  @Override
  public Set<UUID> toKeys(Set<? extends ITypedId<?>> references) {
    return references.stream().map(this::toKey).collect(Collectors.toSet());
  }

  @Override
  public String schemaReferenceOf(String typeSingularTerm) {
    return linkTo(
            methodOn(EntitySchemaController.class)
                .getSchema(ANY_AUTH, typeSingularTerm, ANY_UUID_LIST))
        .withSelfRel()
        .getHref();
  }

  private UnprocessableDataException invalidReference(String uri) {
    return new UnprocessableDataException("Invalid entity reference: %s".formatted(uri));
  }

  private static UriComponents createTemplate(Link dummyLink) {
    return UriComponentsBuilder.fromUriString(
            trimVariables(dummyLink.getHref()).replace(DUMMY_UUID_STRING, "{id}"))
        .build();
  }

  private String buildUri(UriComponents template, Object... params) {
    UriComponentsBuilder b = createUriComponentsBuilder();
    return b.pathSegment(template.getPathSegments().toArray(String[]::new))
        .buildAndExpand(params)
        .toUriString();
  }

  private UriComponentsBuilder createUriComponentsBuilder() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    boolean inRequestContext = requestAttributes != null;
    if (inRequestContext) {
      if (requestAttributes instanceof ServletRequestAttributes sra) {
        return ServletUriComponentsBuilder.fromContextPath(sra.getRequest());
      } else {
        throw new IllegalStateException("No current ServletRequestAttributes");
      }
    } else {
      return UriComponentsBuilder.fromPath("/");
    }
  }
}
