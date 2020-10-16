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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.dto.AbstractAssetDto;
import org.veo.adapter.presenter.api.dto.AbstractClientDto;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.AbstractDocumentDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractPersonDto;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
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
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.transform.ClassKey;
import org.veo.core.entity.transform.EntityFactory;

/**
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class DtoToEntityTransformer {

    // PersonDto->Person
    public static Person transformDto2Person(DtoToEntityContext tcontext, AbstractPersonDto source,
            Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Person.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Person target = (Person) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createPerson(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.PERSON);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }

        return target;
    }

    public static PersonGroup transformDto2PersonGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Person> source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(PersonGroup.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        PersonGroup target = (PersonGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createPersonGroup();
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.PERSON);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> e.findInContext(context))
                                .collect(Collectors.toSet()));

        return target;
    }

    // AssetDto->Asset
    public static Asset transformDto2Asset(DtoToEntityContext tcontext, AbstractAssetDto source,
            Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Asset.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Asset target = (Asset) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createAsset(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.ASSET);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }

        return target;
    }

    public static AssetGroup transformDto2AssetGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Asset> source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(AssetGroup.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        AssetGroup target = (AssetGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createAssetGroup();
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.ASSET);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> e.findInContext(context))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ProcessDto->Process
    public static Process transformDto2Process(DtoToEntityContext tcontext,
            AbstractProcessDto source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Process.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Process target = (Process) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createProcess(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.PROCESS);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }

        return target;
    }

    public static ProcessGroup transformDto2ProcessGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Process> source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(EntityLayerSupertypeGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ProcessGroup target = (ProcessGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createProcessGroup();
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.PROCESS);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> e.findInContext(context))
                                .collect(Collectors.toSet()));

        return target;
    }

    // DocumentDto->Document
    public static Document transformDto2Document(DtoToEntityContext tcontext,
            AbstractDocumentDto source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Document.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Document target = (Document) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createDocument(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.DOCUMENT);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }

        return target;
    }

    public static DocumentGroup transformDto2DocumentGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Document> source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(DocumentGroup.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        DocumentGroup target = (DocumentGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createDocumentGroup();
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e1 -> e1.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.DOCUMENT);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> e.findInContext(context))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ControlDto->Control
    public static Control transformDto2Control(DtoToEntityContext tcontext,
            AbstractControlDto source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Control.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Control target = (Control) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createControl(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.CONTROL);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }

        return target;
    }

    public static ControlGroup transformDto2ControlGroup(DtoToEntityContext tcontext,
            EntityLayerSupertypeGroupDto<Control> source, Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ControlGroup.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ControlGroup target = (ControlGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createControlGroup();
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        var customAttributesTransformer = tcontext.createCustomAttributesTransformer(EntityTypeNames.CONTROL);
        target.setLinks(mapLinks(tcontext, target, source, customAttributesTransformer));
        target.setCustomAspects(mapCustomAspects(source, tcontext.getFactory(),
                                                 customAttributesTransformer));
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner()
                                  .findInContext(context));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> e.findInContext(context))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ClientDto->Client
    public static Client transformDto2Client(DtoToEntityContext tcontext, AbstractClientDto source,
            Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Client.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Client target = (Client) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createClient(key, source.getName());
        target.setId(key);
        target.setName(source.getName());
        context.put(classKey, target);
        target.setDomains(convertSet(source.getDomains(), e -> e.toEntity(tcontext)));

        return target;
    }

    // DomainDto->Domain
    public static Domain transformDto2Domain(DtoToEntityContext tcontext, AbstractDomainDto source,
            Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Domain.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Domain target = (Domain) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createDomain(key, source.getName());
        target.setId(key);
        mapNameableProperties(source, target);
        target.setActive(source.isActive());
        context.put(classKey, target);

        return target;
    }

    // UnitDto->Unit
    public static Unit transformDto2Unit(DtoToEntityContext tcontext, AbstractUnitDto source,
            Key<UUID> key) {
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Unit.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Unit target = (Unit) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = tcontext.getFactory()
                         .createUnit(key, source.getName(), null);
        target.setId(key);
        mapNameableProperties(source, target);
        context.put(classKey, target);

        target.setDomains(convertSet(source.getDomains(), e -> e.findInContext(context)));
        if (source.getClient() != null) {
            target.setClient(source.getClient()
                                   .findInContext(context));
        }
        if (source.getParent() != null) {
            target.setParent(source.getParent()
                                   .findInContext(context));
        }

        return target;
    }

    // CustomLinkDto->CustomLink
    public static CustomLink transformDto2CustomLink(DtoToEntityContext tcontext,
            CustomLinkDto source, String type,
            CustomAttributesTransformer customAttributesTransformer) {
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        EntityLayerSupertype linkTarget = null;
        if (source.getTarget() != null) {
            linkTarget = source.getTarget()
                               .findInContext(context);
        }

        var target = tcontext.getFactory()
                             .createCustomLink(source.getName(), linkTarget, null);

        target.setApplicableTo(source.getApplicableTo());
        target.setType(type);
        mapNameableProperties(source, target);
        customAttributesTransformer.applyLinkAttributes(source.getAttributes(), target);

        return target;

    }

    // CustomPropertiesDto->CustomProperties
    public static CustomProperties transformDto2CustomProperties(EntityFactory factory,
            CustomPropertiesDto source, String type,
            CustomAttributesTransformer customAttributesTransformer) {
        var target = factory.createCustomProperties();
        target.setApplicableTo(source.getApplicableTo());
        target.setType(type);
        customAttributesTransformer.applyAspectAttributes(source.getAttributes(), target);
        return target;

    }

    private static Set<CustomLink> mapLinks(DtoToEntityContext context, EntityLayerSupertype entity,
            EntityLayerSupertypeDto dto, CustomAttributesTransformer customAttributesTransformer) {
        return dto.getLinks()
                  .entrySet()
                  .stream()
                  .flatMap(entry -> entry.getValue()
                                         .stream()
                                         .map(linktDto -> {
                                             var customLink = linktDto.toEntity(context,
                                                                                entry.getKey(),
                                                                                customAttributesTransformer);
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
            EntityFactory factory, CustomAttributesTransformer customAttributesTransformer) {
        return dto.getCustomAspects()
                  .entrySet()
                  .stream()
                  .map(entry -> entry.getValue()
                                     .toEntity(factory, entry.getKey(),
                                               customAttributesTransformer))
                  .collect(Collectors.toSet());
    }

    private DtoToEntityTransformer() {
    }
}
