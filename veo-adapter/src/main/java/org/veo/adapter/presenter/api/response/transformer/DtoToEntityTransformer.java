/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.adapter.presenter.api.response.transformer;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.dto.AbstractAssetDto;
import org.veo.adapter.presenter.api.dto.AbstractClientDto;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.AbstractDocumentDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractIncidentDto;
import org.veo.adapter.presenter.api.dto.AbstractPersonDto;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
import org.veo.adapter.presenter.api.dto.AbstractScenarioDto;
import org.veo.adapter.presenter.api.dto.AbstractScopeDto;
import org.veo.adapter.presenter.api.dto.AbstractUnitDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.CustomPropertiesDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeEntity;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.EntityFactory;

/**
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class DtoToEntityTransformer {

    // PersonDto->Person
    public Person transformDto2Person(DtoToEntityContext tcontext, AbstractPersonDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createPerson(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    // AssetDto->Asset
    public Asset transformDto2Asset(DtoToEntityContext tcontext, AbstractAssetDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createAsset(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    // ProcessDto->Process
    public Process transformDto2Process(DtoToEntityContext tcontext, AbstractProcessDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createProcess(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    // DocumentDto->Document
    public Document transformDto2Document(DtoToEntityContext tcontext, AbstractDocumentDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createDocument(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    // ControlDto->Control
    public Control transformDto2Control(DtoToEntityContext tcontext, AbstractControlDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createControl(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    // IncidentDto->Incident
    public Incident transformDto2Incident(DtoToEntityContext tcontext, AbstractIncidentDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createIncident(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    // ScenarioDto->Scenario
    public Scenario transformDto2Scenario(DtoToEntityContext tcontext, AbstractScenarioDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createScenario(key, source.getName(), null);
        mapCompositeEntity(tcontext, source, target);
        return target;
    }

    public Scope transformDto2Scope(DtoToEntityContext tcontext, AbstractScopeDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createScope(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        target.setMembers(source.getMembers()
                                .stream()
                                .map(tcontext::resolve)
                                .collect(Collectors.toSet()));
        return target;
    }

    // ClientDto->Client
    public Client transformDto2Client(DtoToEntityContext tcontext, AbstractClientDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createClient(key, source.getName());
        target.setId(key);
        target.setName(source.getName());
        target.setDomains(convertSet(source.getDomains(),
                                     e -> transformDto2Domain(tcontext, e,
                                                              e instanceof IdentifiableDto
                                                                      ? Key.uuidFrom(((IdentifiableDto) e).getId())
                                                                      : null)));

        return target;
    }

    // DomainDto->Domain
    public Domain transformDto2Domain(DtoToEntityContext tcontext, AbstractDomainDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createDomain(key, source.getName());
        target.setId(key);
        mapNameableProperties(source, target);
        target.setActive(source.isActive());

        return target;
    }

    // UnitDto->Unit
    public Unit transformDto2Unit(DtoToEntityContext tcontext, AbstractUnitDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createUnit(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);

        target.setDomains(convertSet(source.getDomains(), tcontext::resolve));
        if (source.getClient() != null) {
            target.setClient(tcontext.resolve(source.getClient()));
        }
        if (source.getParent() != null) {
            target.setParent(tcontext.resolve(source.getParent()));
        }

        return target;
    }

    // CustomLinkDto->CustomLink
    public CustomLink transformDto2CustomLink(DtoToEntityContext tcontext, CustomLinkDto source,
            String type, EntitySchema entitySchema) {
        EntityLayerSupertype linkTarget = null;
        if (source.getTarget() != null) {
            linkTarget = tcontext.resolve(source.getTarget());
        }

        var target = tcontext.getFactory()
                             .createCustomLink(source.getName(), linkTarget, null);

        target.setApplicableTo(source.getApplicableTo());
        target.setType(type);
        mapNameableProperties(source, target);
        entitySchema.applyLinkAttributes(source.getAttributes(), target);
        entitySchema.validateLinkTarget(target);
        return target;

    }

    // CustomPropertiesDto->CustomProperties
    public CustomProperties transformDto2CustomProperties(EntityFactory factory,
            CustomPropertiesDto source, String type, EntitySchema entitySchema) {
        var target = factory.createCustomProperties();
        target.setApplicableTo(source.getApplicableTo());
        target.setType(type);
        entitySchema.applyAspectAttributes(source.getAttributes(), target);
        return target;
    }

    private <T extends EntityLayerSupertype> void mapCompositeEntity(DtoToEntityContext tcontext,
            CompositeEntityDto<T> source, CompositeEntity<T> target) {
        mapEntityLayerSupertype(tcontext, source, target);
        target.setParts(source.getParts()
                              .stream()
                              .map(tcontext::resolve)
                              .collect(Collectors.toSet()));
    }

    private <TDto extends EntityLayerSupertypeDto, TEntity extends EntityLayerSupertype> void mapEntityLayerSupertype(
            DtoToEntityContext tcontext, TDto source, TEntity target) {
        mapNameableProperties(source, target);
        target.setDomains(convertSet(source.getDomains(), tcontext::resolve));
        tcontext.getSubTypeTransformer()
                .mapSubTypesToEntity(source, target);
        var entitySchema = tcontext.loadEntitySchema(target.getModelType());
        target.setLinks(mapLinks(tcontext, target, source, entitySchema));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(), entitySchema));
        if (source.getOwner() != null) {
            target.setOwner(tcontext.resolve(source.getOwner()));
        }
    }

    private Set<CustomLink> mapLinks(DtoToEntityContext context, EntityLayerSupertype entity,
            EntityLayerSupertypeDto dto, EntitySchema entitySchema) {
        return dto.getLinks()
                  .entrySet()
                  .stream()
                  .flatMap(entry -> entry.getValue()
                                         .stream()
                                         .map(linktDto -> {
                                             var customLink = transformDto2CustomLink(context,
                                                                                      linktDto,
                                                                                      entry.getKey(),
                                                                                      entitySchema);
                                             customLink.setSource(entity);
                                             return customLink;
                                         }))
                  .collect(toSet());
    }

    private static void mapNameableProperties(NameableDto source, Nameable target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static <TIn, TOut extends ModelObject> Set<TOut> convertSet(Set<TIn> source,
            Function<TIn, TOut> mapper) {
        if (mapper != null) {
            return source.stream()
                         .map(mapper)
                         .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    private Set<CustomProperties> mapCustomAspects(EntityLayerSupertypeDto dto,
            EntityFactory factory, EntitySchema entitySchema) {
        return dto.getCustomAspects()
                  .entrySet()
                  .stream()
                  .map(entry -> transformDto2CustomProperties(factory, entry.getValue(),
                                                              entry.getKey(), entitySchema))
                  .collect(Collectors.toSet());
    }

}
