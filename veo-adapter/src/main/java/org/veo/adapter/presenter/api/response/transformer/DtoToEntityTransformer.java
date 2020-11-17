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
import org.veo.adapter.presenter.api.dto.AbstractUnitDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.CustomPropertiesDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Unit;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.IncidentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.groups.ScenarioGroup;
import org.veo.core.entity.transform.EntityFactory;

/**
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class DtoToEntityTransformer {

    // PersonDto->Person
    public static Person transformDto2Person(DtoToEntityContext tcontext, AbstractPersonDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createPerson(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static PersonGroup transformDto2PersonGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Person> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createPersonGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // AssetDto->Asset
    public static Asset transformDto2Asset(DtoToEntityContext tcontext, AbstractAssetDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createAsset(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static AssetGroup transformDto2AssetGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Asset> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createAssetGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // ProcessDto->Process
    public static Process transformDto2Process(DtoToEntityContext tcontext,
            AbstractProcessDto source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createProcess(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static ProcessGroup transformDto2ProcessGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Process> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createProcessGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // DocumentDto->Document
    public static Document transformDto2Document(DtoToEntityContext tcontext,
            AbstractDocumentDto source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createDocument(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static DocumentGroup transformDto2DocumentGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Document> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createDocumentGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // ControlDto->Control
    public static Control transformDto2Control(DtoToEntityContext tcontext,
            AbstractControlDto source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createControl(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static ControlGroup transformDto2ControlGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Control> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createControlGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // IncidentDto->Incident
    public static Incident transformDto2Incident(DtoToEntityContext tcontext,
            AbstractIncidentDto source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createIncident(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static IncidentGroup transformDto2IncidentGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Incident> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createIncidentGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // ScenarioDto->Scenario
    public static Scenario transformDto2Scenario(DtoToEntityContext tcontext,
            AbstractScenarioDto source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createScenario(key, source.getName(), null);
        mapEntityLayerSupertype(tcontext, source, target);
        return target;
    }

    public static ScenarioGroup transformDto2ScenarioGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Scenario> source, Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createScenarioGroup(key, source.getName(), null);
        mapEntityLayerSupertypeGroup(tcontext, source, target);
        return target;
    }

    // ClientDto->Client
    public static Client transformDto2Client(DtoToEntityContext tcontext, AbstractClientDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createClient(key, source.getName());
        target.setId(key);
        target.setName(source.getName());
        target.setDomains(convertSet(source.getDomains(), e -> e.toEntity(tcontext)));

        return target;
    }

    // DomainDto->Domain
    public static Domain transformDto2Domain(DtoToEntityContext tcontext, AbstractDomainDto source,
            Key<UUID> key) {
        var target = tcontext.getFactory()
                             .createDomain(key, source.getName());
        target.setId(key);
        mapNameableProperties(source, target);
        target.setActive(source.isActive());

        return target;
    }

    // UnitDto->Unit
    public static Unit transformDto2Unit(DtoToEntityContext tcontext, AbstractUnitDto source,
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
    public static CustomLink transformDto2CustomLink(DtoToEntityContext tcontext,
            CustomLinkDto source, String type, EntitySchema entitySchema) {
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
    public static CustomProperties transformDto2CustomProperties(EntityFactory factory,
            CustomPropertiesDto source, String type, EntitySchema entitySchema) {
        var target = factory.createCustomProperties();
        target.setApplicableTo(source.getApplicableTo());
        target.setType(type);
        entitySchema.applyAspectAttributes(source.getAttributes(), target);
        return target;
    }

    private static <T extends EntityLayerSupertype> void mapEntityLayerSupertypeGroup(
            DtoToEntityContext tcontext, EntityLayerSupertypeGroupDto<T> source,
            ModelGroup<T> target) {
        mapEntityLayerSupertype(tcontext, source, target);
        target.setMembers(source.getMembers()
                                .stream()
                                .map(tcontext::resolve)
                                .collect(Collectors.toSet()));
    }

    private static <TDto extends EntityLayerSupertypeDto, TEntity extends EntityLayerSupertype> void mapEntityLayerSupertype(
            DtoToEntityContext tcontext, TDto source, TEntity target) {
        mapNameableProperties(source, target);
        target.setDomains(convertSet(source.getDomains(), tcontext::resolve));
        var entitySchema = tcontext.loadEntitySchema(target.getModelType());
        target.setLinks(mapLinks(tcontext, target, source, entitySchema));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(), entitySchema));
        if (source.getOwner() != null) {
            target.setOwner(tcontext.resolve(source.getOwner()));
        }
    }

    private static Set<CustomLink> mapLinks(DtoToEntityContext context, EntityLayerSupertype entity,
            EntityLayerSupertypeDto dto, EntitySchema entitySchema) {
        return dto.getLinks()
                  .entrySet()
                  .stream()
                  .flatMap(entry -> entry.getValue()
                                         .stream()
                                         .map(linktDto -> {
                                             var customLink = linktDto.toEntity(context,
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

    private static Set<CustomProperties> mapCustomAspects(EntityLayerSupertypeDto dto,
            EntityFactory factory, EntitySchema entitySchema) {
        return dto.getCustomAspects()
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getValue()
                                     .toEntity(factory, entry.getKey(), entitySchema))
                  .collect(Collectors.toSet());
    }

    private DtoToEntityTransformer() {
    }
}
