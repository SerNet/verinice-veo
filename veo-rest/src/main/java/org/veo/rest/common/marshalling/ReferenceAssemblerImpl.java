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
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.veo.rest.ControllerConstants.ANY_BOOLEAN;
import static org.veo.rest.ControllerConstants.ANY_INT;
import static org.veo.rest.ControllerConstants.ANY_LONG;
import static org.veo.rest.ControllerConstants.ANY_REQUEST;
import static org.veo.rest.ControllerConstants.ANY_STRING;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
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
import org.veo.rest.ContentCreationController;
import org.veo.rest.ControlController;
import org.veo.rest.ControlInDomainController;
import org.veo.rest.DocumentController;
import org.veo.rest.DocumentInDomainController;
import org.veo.rest.DomainController;
import org.veo.rest.IncidentController;
import org.veo.rest.IncidentInDomainController;
import org.veo.rest.MessageController;
import org.veo.rest.PersonController;
import org.veo.rest.PersonInDomainController;
import org.veo.rest.ProcessController;
import org.veo.rest.ProcessInDomainController;
import org.veo.rest.RiskAffectedResource;
import org.veo.rest.ScenarioController;
import org.veo.rest.ScenarioInDomainController;
import org.veo.rest.ScopeController;
import org.veo.rest.ScopeInDomainController;
import org.veo.rest.UnitController;
import org.veo.rest.UserConfigurationController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@SuppressFBWarnings(
    value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS",
    justification = "The controller method invocations are just dummies")
public class ReferenceAssemblerImpl implements ReferenceAssembler {
  private static final String DUMMY_UUID_STRING = "00000000-0000-0000-0000-000000000000";
  private static final UUID DUMMY_UUID = UUID.fromString(DUMMY_UUID_STRING);

  private static final UriComponents GET_ASSET =
      createTemplate(on(AssetController.class).getAsset(DUMMY_UUID, ANY_BOOLEAN, ANY_REQUEST));

  private static final UriComponents GET_CONTROL =
      createTemplate(on(ControlController.class).getElement(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_DOCUMENT =
      createTemplate(on(DocumentController.class).getElement(DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_DOMAIN =
      createTemplate(on(DomainController.class).getDomain(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_DOMAIN_TEMPLATE =
      createTemplate(on(ContentCreationController.class).getDomainTemplate(DUMMY_UUID));

  private static final UriComponents GET_INCIDENT =
      createTemplate(on(IncidentController.class).getElement(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_PROFILE =
      createTemplate(on(DomainController.class).getProfile(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_PERSON =
      createTemplate(on(PersonController.class).getElement(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_PROCESS =
      createTemplate(on(ProcessController.class).getProcess(DUMMY_UUID, ANY_BOOLEAN, ANY_REQUEST));

  private static final UriComponents GET_SCENARIO =
      createTemplate(on(ScenarioController.class).getElement(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_SCOPE =
      createTemplate(on(ScopeController.class).getScope(DUMMY_UUID, ANY_BOOLEAN, ANY_REQUEST));

  private static final UriComponents GET_UNIT =
      createTemplate(on(UnitController.class).getUnit(DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_ASSET_IN_DOMAIN =
      createTemplate(
          on(AssetInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_CONTROL_IN_DOMAIN =
      createTemplate(
          on(ControlInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_DOCUMENT_IN_DOMAIN =
      createTemplate(
          on(DocumentInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_INCIDENT_IN_DOMAIN =
      createTemplate(
          on(IncidentInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_PERSON_IN_DOMAIN =
      createTemplate(
          on(PersonInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_PROCESS_IN_DOMAIN =
      createTemplate(
          on(ProcessInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_SCENARIO_IN_DOMAIN =
      createTemplate(
          on(ScenarioInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));
  private static final UriComponents GET_SCOPE_IN_DOMAIN =
      createTemplate(
          on(ScopeInDomainController.class).getElement(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_ASSET_RI =
      createTemplate(linkToRequirementImplementation(AssetController.class));
  private static final UriComponents GET_PROCESS_RI =
      createTemplate(linkToRequirementImplementation(ProcessController.class));
  private static final UriComponents GET_SCOPE_RI =
      createTemplate(linkToRequirementImplementation(ScopeController.class));

  private static final UriComponents GET_ASSET_RIS =
      createTemplate(linkToRequirementImplementations(AssetController.class));
  private static final UriComponents GET_PROCESS_RIS =
      createTemplate(linkToRequirementImplementations(ProcessController.class));
  private static final UriComponents GET_SCOPE_RIS =
      createTemplate(linkToRequirementImplementations(ScopeController.class));

  private static final UriComponents GET_INSPECTION =
      createTemplate(on(DomainController.class).getInspection(DUMMY_UUID, DUMMY_UUID_STRING));

  private static final UriComponents GET_CATALOG_ITEM =
      createTemplate(
          on(DomainController.class).getCatalogItem(DUMMY_UUID, DUMMY_UUID, ANY_REQUEST));

  private static final UriComponents GET_PROFILE_ITEM =
      createTemplate(on(DomainController.class).getProfileItem(DUMMY_UUID, DUMMY_UUID, DUMMY_UUID));

  private static final UriComponents GET_ASSET_RISK =
      createTemplate(on(AssetController.class).getRisk(DUMMY_UUID, DUMMY_UUID));

  private static final UriComponents GET_PROCESS_RISK =
      createTemplate(on(ProcessController.class).getRisk(DUMMY_UUID, DUMMY_UUID));

  private static final UriComponents GET_SCOPE_RISK =
      createTemplate(on(ScopeController.class).getRisk(DUMMY_UUID, DUMMY_UUID));

  private static final UriComponents GET_USER_CONFIGURATION =
      createTemplate(on(UserConfigurationController.class).getUserConfiguration(ANY_STRING));

  private static final UriComponents GET_SYSTEM_MESSAGE =
      createTemplate(on(MessageController.class).getSystemMessage(ANY_LONG));

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
    return elementInDomainRefOf(TypedId.from(element), domain.getId());
  }

  @Override
  public String elementInDomainRefOf(ITypedId<Element> reference, UUID domainId) {
    var type = reference.getType();
    if (Asset.class.isAssignableFrom(type)) {
      return buildUri(GET_ASSET_IN_DOMAIN, domainId, reference.getId());
    }
    if (Control.class.isAssignableFrom(type)) {
      return buildUri(GET_CONTROL_IN_DOMAIN, domainId, reference.getId());
    }
    if (Document.class.isAssignableFrom(type)) {
      return buildUri(GET_DOCUMENT_IN_DOMAIN, domainId, reference.getId());
    }
    if (Incident.class.isAssignableFrom(type)) {
      return buildUri(GET_INCIDENT_IN_DOMAIN, domainId, reference.getId());
    }
    if (Person.class.isAssignableFrom(type)) {
      return buildUri(GET_PERSON_IN_DOMAIN, domainId, reference.getId());
    }
    if (Process.class.isAssignableFrom(type)) {
      return buildUri(GET_PROCESS_IN_DOMAIN, domainId, reference.getId());
    }
    if (Scenario.class.isAssignableFrom(type)) {
      return buildUri(GET_SCENARIO_IN_DOMAIN, domainId, reference.getId());
    }
    if (Scope.class.isAssignableFrom(type)) {
      return buildUri(GET_SCOPE_IN_DOMAIN, domainId, reference.getId());
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

  private static Object linkToRequirementImplementation(
      Class<? extends RiskAffectedResource> controller) {
    return on(controller).getRequirementImplementation(DUMMY_UUID, DUMMY_UUID);
  }

  private static Object linkToRequirementImplementations(
      Class<? extends RiskAffectedResource> controller) {
    return on(controller)
        .getRequirementImplementations(
            DUMMY_UUID, DUMMY_UUID, ANY_INT, ANY_INT, ANY_STRING, ANY_STRING);
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
      var typeSeg = segments.get(size - 2);
      if (typeSeg.equals(Client.PLURAL_TERM)) {
        throw invalidReference(uri);
      }
      var idSeg = segments.get(size - 1);
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

  private UnprocessableDataException invalidReference(String uri) {
    return new UnprocessableDataException("Invalid entity reference: %s".formatted(uri));
  }

  private static UriComponents createTemplate(Object info) {
    return UriComponentsBuilder.fromUriString(
            MvcUriComponentsBuilder.fromMethodCall(UriComponentsBuilder.fromPath("/"), info)
                .toUriString()
                .replace(DUMMY_UUID_STRING, "{id}"))
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
