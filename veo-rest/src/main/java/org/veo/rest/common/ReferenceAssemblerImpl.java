/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest.common;

import static java.lang.String.format;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.ANY_SEARCH;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.ANY_USER;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.CompoundKeyEntity;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.ModelObjectType;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.rest.AssetController;
import org.veo.rest.AssetRiskResource;
import org.veo.rest.ControlController;
import org.veo.rest.DocumentController;
import org.veo.rest.IncidentController;
import org.veo.rest.PersonController;
import org.veo.rest.ProcessController;
import org.veo.rest.ScenarioController;
import org.veo.rest.ScopeController;
import org.veo.rest.UnitController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@NoArgsConstructor
@Slf4j
public class ReferenceAssemblerImpl implements ReferenceAssembler {

    private static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    @Override
    @SuppressFBWarnings // ignore warning on call to method proxy factory
    public String targetReferenceOf(Class<? extends ModelObject> type, String id) {
        if (Scope.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ScopeController.class).getScope(ANY_AUTH,
                                                                   id)).withRel(ScopeController.URL_BASE_PATH)
                                                                       .getHref();
        }
        if (Asset.class.isAssignableFrom(type)) {
            return linkTo(methodOn(AssetController.class).getAsset(ANY_AUTH,
                                                                   id)).withRel(AssetController.URL_BASE_PATH)
                                                                       .getHref();
        }
        if (Document.class.isAssignableFrom(type)) {
            return linkTo(methodOn(DocumentController.class).getDocument(ANY_AUTH,
                                                                         id)).withRel(DocumentController.URL_BASE_PATH)
                                                                             .getHref();
        }
        if (Unit.class.isAssignableFrom(type)) {
            return linkTo(methodOn(UnitController.class).getUnit(ANY_AUTH,
                                                                 id)).withRel(UnitController.URL_BASE_PATH)
                                                                     .getHref();
        }
        if (Person.class.isAssignableFrom(type)) {
            return linkTo(methodOn(PersonController.class).getPerson(ANY_AUTH,
                                                                     id)).withRel(PersonController.URL_BASE_PATH)
                                                                         .getHref();
        }
        if (Process.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ProcessController.class).getProcessById(ANY_AUTH,
                                                                           id)).withRel(ProcessController.URL_BASE_PATH)
                                                                               .getHref();
        }
        if (Control.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ControlController.class).getControl(ANY_AUTH,
                                                                       id)).withRel(ControlController.URL_BASE_PATH)
                                                                           .getHref();
        }
        if (Scenario.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ScenarioController.class).getScenario(ANY_AUTH,
                                                                         id)).withRel(ScenarioController.URL_BASE_PATH)
                                                                             .getHref();
        }
        if (Incident.class.isAssignableFrom(type)) {
            return linkTo(methodOn(IncidentController.class).getIncident(ANY_AUTH,
                                                                         id)).withRel(IncidentController.URL_BASE_PATH)
                                                                             .getHref();
        }
        // FIXME VEO-227 There is no ressource endpoint for Domain objects so we
        // cannot
        // create a URL to them.
        if (Domain.class.isAssignableFrom(type)) {
            return "/" + Domain.PLURAL_TERM + "/" + id;
        }

        return "";
    }

    @Override
    @SuppressFBWarnings // ignore warning on call to method proxy factory
    public String targetReferenceOf(Class<? extends CompoundKeyEntity> type, String id1,
            String id2) {
        if (AssetRisk.class.isAssignableFrom(type)) {
            return linkTo(methodOn(AssetController.class).getRisk(ANY_USER, id1,
                                                                  id2)).withRel(AssetController.URL_BASE_PATH
                                                                          + AssetRiskResource.RELPATH)
                                                                       .getHref();
        }
        throw new NotImplementedException(
                format("Cannot create compound-id reference to entity " + "%s.",
                       type.getSimpleName()));
    }

    @Override
    public String searchesReferenceOf(Class<? extends ModelObject> type) {
        if (Scope.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ScopeController.class).createSearch(ANY_AUTH,
                                                                       ANY_SEARCH)).withRel(ScopeController.URL_BASE_PATH)
                                                                                   .getHref();
        }
        if (Asset.class.isAssignableFrom(type)) {
            return linkTo(methodOn(AssetController.class).createSearch(ANY_AUTH,
                                                                       ANY_SEARCH)).withRel(AssetController.URL_BASE_PATH)
                                                                                   .getHref();
        }
        if (Unit.class.isAssignableFrom(type)) {
            return linkTo(methodOn(UnitController.class).createSearch(ANY_AUTH,
                                                                      ANY_SEARCH)).withRel(UnitController.URL_BASE_PATH)
                                                                                  .getHref();
        }
        if (Process.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ProcessController.class).createSearch(ANY_AUTH,
                                                                         ANY_SEARCH)).withRel(ProcessController.URL_BASE_PATH)
                                                                                     .getHref();
        }
        if (Person.class.isAssignableFrom(type)) {
            return linkTo(methodOn(PersonController.class).createSearch(ANY_AUTH,
                                                                        ANY_SEARCH)).withRel(PersonController.URL_BASE_PATH)
                                                                                    .getHref();
        }
        if (Control.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ControlController.class).createSearch(ANY_AUTH,
                                                                         ANY_SEARCH)).withRel(ControlController.URL_BASE_PATH)
                                                                                     .getHref();
        }
        if (Scenario.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ScenarioController.class).createSearch(ANY_AUTH,
                                                                          ANY_SEARCH)).withRel(ScenarioController.URL_BASE_PATH)
                                                                                      .getHref();
        }
        if (Incident.class.isAssignableFrom(type)) {
            return linkTo(methodOn(IncidentController.class).createSearch(ANY_AUTH,
                                                                          ANY_SEARCH)).withRel(IncidentController.URL_BASE_PATH)
                                                                                      .getHref();
        }
        return "";
    }

    @Override
    @SuppressFBWarnings // ignore warnings on calls to method proxy factories
    public String resourcesReferenceOf(Class<? extends ModelObject> type) {
        if (Scope.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ScopeController.class).getScopes(ANY_AUTH, ANY_STRING,
                                                                    ANY_STRING, ANY_STRING))
                                                                                            .withSelfRel()
                                                                                            .getHref();
        }
        if (Asset.class.isAssignableFrom(type)) {
            return linkTo(methodOn(AssetController.class).getAssets(ANY_AUTH, ANY_STRING,
                                                                    ANY_STRING, ANY_STRING))
                                                                                            .withSelfRel()
                                                                                            .getHref();
        }
        if (Unit.class.isAssignableFrom(type)) {
            return linkTo(methodOn(UnitController.class).getUnits(ANY_AUTH, ANY_STRING,
                                                                  ANY_STRING)).withSelfRel()
                                                                              .getHref();
        }
        if (Process.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ProcessController.class).getProcesses(ANY_AUTH, ANY_STRING,
                                                                         ANY_STRING, ANY_STRING))
                                                                                                 .withSelfRel()
                                                                                                 .getHref();
        }
        if (Person.class.isAssignableFrom(type)) {
            return linkTo(methodOn(PersonController.class).getPersons(ANY_AUTH, ANY_STRING,
                                                                      ANY_STRING, ANY_STRING))
                                                                                              .withSelfRel()
                                                                                              .getHref();
        }
        if (Control.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ControlController.class).getControls(ANY_AUTH, ANY_STRING,
                                                                        ANY_STRING, ANY_STRING))
                                                                                                .withSelfRel()
                                                                                                .getHref();
        }
        if (Scenario.class.isAssignableFrom(type)) {
            return linkTo(methodOn(ScenarioController.class).getScenarios(ANY_AUTH, ANY_STRING,
                                                                          ANY_STRING, ANY_STRING))
                                                                                                  .withSelfRel()
                                                                                                  .getHref();
        }
        if (Incident.class.isAssignableFrom(type)) {
            return linkTo(methodOn(IncidentController.class).getIncidents(ANY_AUTH, ANY_STRING,
                                                                          ANY_STRING, ANY_STRING))
                                                                                                  .withSelfRel()
                                                                                                  .getHref();
        }
        // all types not listed above do not support access to a resource
        // collection.
        // Returning 'null' as per the method contract:
        return null;
    }

    @Override
    public Class<? extends ModelObject> parseType(String uriString) {
        Optional<String> term = readObjectTypePluralTerm(uriString);
        return ModelObjectType.getTypeForPluralTerm(term.orElseThrow());
    }

    @Override
    public String parseId(String uriString) {
        Optional<String> objectTypeTerm = readObjectTypePluralTerm(uriString);
        var pat = Pattern.compile(String.join("", ".*/", objectTypeTerm.orElseThrow(), "/",
                                              "(?<resourceId>", UUID_REGEX, ").*"));
        var matcher = pat.matcher(uriString);
        matcher.find();
        return matcher.group("resourceId");
    }

    @Override
    public Key<UUID> toKey(ModelObjectReference<? extends ModelObject> reference) {
        if (reference == null)
            return null;
        return Key.uuidFrom(parseId(reference.getTargetUri()));
    }

    @Override
    public Set<Key<UUID>> toKeys(Set<? extends ModelObjectReference<?>> references) {
        return references.stream()
                         .map(this::toKey)
                         .collect(Collectors.toSet());
    }

    private Optional<String> readObjectTypePluralTerm(String uriString) {
        return ModelObjectType.PLURAL_TERMS.stream()
                                           .filter(t -> readUriString(uriString).matches(String.join("",
                                                                                                     ".*/",
                                                                                                     t,
                                                                                                     "/",
                                                                                                     UUID_REGEX,
                                                                                                     ".*")))
                                           .findFirst();
    }

    /**
     * This method tries to create a URL from the input string. If that doesn't
     * work, it tries to create a URI. We do this because we also accept URIs as
     * references (i.e. relative URIs).
     *
     * @throws IllegalArgumentException
     *             if no path could be extracted from neither URI nor URL
     *
     * @see URL
     * @see URI
     */
    private String readUriString(String uriString) {
        try {
            return parseURL(uriString);
        } catch (MalformedURLException e) {
            return parseURI(uriString);
        }
    }

    private String parseURI(String uriString) {
        try {
            var uri = new URI(uriString);
            return uri.getPath();
        } catch (URISyntaxException e) {
            log.error(format("Could not parse URI for element: %s", uriString), e);
            throw new IllegalArgumentException(
                    format("Could not parse URI for element: %s", uriString));
        }
    }

    private String parseURL(String uriString) throws MalformedURLException {
        try {
            return new URL(uriString).getPath();
        } catch (MalformedURLException e) {
            log.info(format("Reference is not a valid URL: %s", uriString));
            throw e;
        }
    }
}
