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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.AssetDto;
import org.veo.adapter.presenter.api.response.BaseModelObjectDto;
import org.veo.adapter.presenter.api.response.ClientDto;
import org.veo.adapter.presenter.api.response.ControlDto;
import org.veo.adapter.presenter.api.response.CustomLinkDto;
import org.veo.adapter.presenter.api.response.CustomPropertiesDto;
import org.veo.adapter.presenter.api.response.DocumentDto;
import org.veo.adapter.presenter.api.response.DomainDto;
import org.veo.adapter.presenter.api.response.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.response.NameAbleDto;
import org.veo.adapter.presenter.api.response.PersonDto;
import org.veo.adapter.presenter.api.response.ProcessDto;
import org.veo.adapter.presenter.api.response.UnitDto;
import org.veo.adapter.presenter.api.response.groups.AssetGroupDto;
import org.veo.adapter.presenter.api.response.groups.ControlGroupDto;
import org.veo.adapter.presenter.api.response.groups.DocumentGroupDto;
import org.veo.adapter.presenter.api.response.groups.EntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.response.groups.PersonGroupDto;
import org.veo.adapter.presenter.api.response.groups.ProcessGroupDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.NameAble;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.custom.LinkImpl;
import org.veo.core.entity.custom.SimpleProperties;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.impl.AssetImpl;
import org.veo.core.entity.impl.ClientImpl;
import org.veo.core.entity.impl.ControlImpl;
import org.veo.core.entity.impl.DocumentImpl;
import org.veo.core.entity.impl.DomainImpl;
import org.veo.core.entity.impl.PersonImpl;
import org.veo.core.entity.impl.ProcessImpl;
import org.veo.core.entity.impl.UnitImpl;
import org.veo.core.entity.transform.ClassKey;

/**
 * A collection of transform functions to transform DTOs to entities.
 */
public final class DtoToEntityTransformer {

    // EntityLayerSupertypeDto->EntityLayerSupertype
    public static EntityLayerSupertype transformDto2EntityLayerSupertype(
            DtoToEntityContext tcontext, EntityLayerSupertypeDto source) {
        EntityLayerSupertypeDto src = source;

        if (src instanceof PersonDto) {
            return transformDto2Person(tcontext, (PersonDto) src);
        }
        if (src instanceof AssetDto) {
            return transformDto2Asset(tcontext, (AssetDto) src);
        }
        if (src instanceof ProcessDto) {
            return transformDto2Process(tcontext, (ProcessDto) src);
        }
        if (src instanceof DocumentDto) {
            return transformDto2Document(tcontext, (DocumentDto) src);
        }
        if (src instanceof ControlDto) {
            return transformDto2Control(tcontext, (ControlDto) src);
        }
        throw new IllegalArgumentException("No transform method defined for " + src.getClass()
                                                                                   .getSimpleName());
    }

    // PersonDto->Person
    public static Person transformDto2Person(DtoToEntityContext tcontext, PersonDto source) {
        if (source instanceof EntityLayerSupertypeGroupDto<?>) {
            return transformDto2PersonGroup(tcontext, (PersonGroupDto) source);
        }
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Person.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Person target = (Person) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonImpl(key, source.getName(), null);
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static PersonGroup transformDto2PersonGroup(DtoToEntityContext tcontext,
            PersonGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(PersonGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        PersonGroup target = (PersonGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // AssetDto->Asset
    public static Asset transformDto2Asset(DtoToEntityContext tcontext, AssetDto source) {
        if (source instanceof EntityLayerSupertypeGroupDto<?>) {
            return transformDto2AssetGroup(tcontext, (AssetGroupDto) source);
        }
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Asset.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Asset target = (Asset) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetImpl(key, source.getName(), null);
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static AssetGroup transformDto2AssetGroup(DtoToEntityContext tcontext,
            AssetGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(AssetGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        AssetGroup target = (AssetGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ProcessDto->Process
    public static Process transformDto2Process(DtoToEntityContext tcontext, ProcessDto source) {
        if (source instanceof EntityLayerSupertypeGroupDto<?>) {
            return transformDto2ProcessGroup(tcontext, (ProcessGroupDto) source);
        }
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Process.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Process target = (Process) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessImpl(key, source.getName(), null);
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static ProcessGroup transformDto2ProcessGroup(DtoToEntityContext tcontext,
            ProcessGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ProcessGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ProcessGroup target = (ProcessGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // DocumentDto->Document
    public static Document transformDto2Document(DtoToEntityContext tcontext, DocumentDto source) {
        if (source instanceof EntityLayerSupertypeGroupDto<?>) {
            return transformDto2DocumentGroup(tcontext, (DocumentGroupDto) source);
        }
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Document.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Document target = (Document) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentImpl(key, source.getName(), null);
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static DocumentGroup transformDto2DocumentGroup(DtoToEntityContext tcontext,
            DocumentGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(DocumentGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        DocumentGroup target = (DocumentGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e1 -> ModelObjectReference.mapToEntity(context, e1)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ControlDto->Control
    public static Control transformDto2Control(DtoToEntityContext tcontext, ControlDto source) {
        if (source instanceof EntityLayerSupertypeGroupDto<?>) {
            return transformDto2ControlGroup(tcontext, (ControlGroupDto) source);
        }
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Control.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Control target = (Control) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlImpl(key, source.getName(), null);
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static ControlGroup transformDto2ControlGroup(DtoToEntityContext tcontext,
            ControlGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ControlGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ControlGroup target = (ControlGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        target.setLinks(map(source.getLinks(), e -> transformDto2CustomLink(tcontext, e)));
        target.setCustomAspects(map(source.getCustomAspects(),
                                    e -> transformDto2CustomProperties(tcontext, e)));
        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ClientDto->Client
    public static Client transformDto2Client(DtoToEntityContext tcontext, ClientDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Client.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Client target = (Client) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ClientImpl(key, source.getName());
        mapProperties(source, target, key);
        target.setName(source.getName());
        context.put(classKey, target);
        Set<Unit> units = map(source.getUnits(), e -> transformDto2Unit(tcontext, e));
        target.setUnits(units);
        target.setDomains(map(source.getDomains(), e -> transformDto2Domain(tcontext, e)));

        return target;
    }

    // DomainDto->Domain
    public static Domain transformDto2Domain(DtoToEntityContext tcontext, DomainDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Domain.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Domain target = (Domain) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DomainImpl(key, source.getName());
        mapProperties(source, target, key);
        mapProperties(source, target);
        target.setActive(source.isActive());
        context.put(classKey, target);

        return target;
    }

    // UnitDto->Unit
    public static Unit transformDto2Unit(DtoToEntityContext tcontext, UnitDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Unit.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Unit target = (Unit) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new UnitImpl(key, source.getName(), null);
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);

        target.setUnits(map(source.getUnits(), e -> transformDto2Unit(tcontext, e)));
        target.setDomains(map(source.getDomains(),
                              e -> ModelObjectReference.mapToEntity(context, e)));
        if (source.getClient() != null) {
            target.setClient(ModelObjectReference.mapToEntity(context, source.getClient()));
        }
        if (source.getParent() != null) {
            target.setParent(ModelObjectReference.mapToEntity(context, source.getParent()));
        }

        return target;
    }

    // CustomLinkDto->CustomLink
    public static CustomLink transformDto2CustomLink(DtoToEntityContext tcontext,
            CustomLinkDto source) {

        Key<UUID> key = source.getId()
                              .equals(Key.NIL_UUID.uuidValue()) ? Key.newUuid()
                                      : Key.uuidFrom(source.getId());// when the object is new we
                                                                     // need to generate a key as
                                                                     // the type is not unique
        ClassKey<Key<UUID>> classKey = new ClassKey<>(CustomLink.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        CustomLink target = (CustomLink) context.get(classKey);
        if (target != null) {
            return target;
        }

        EntityLayerSupertype linkSource = null;
        EntityLayerSupertype linkTarget = null;
        if (source.getTarget() != null) {
            linkTarget = ModelObjectReference.mapToEntity(context, source.getTarget());
        }
        if (source.getSource() != null) {
            linkSource = ModelObjectReference.mapToEntity(context, source.getSource());
        }

        target = new LinkImpl(key, source.getName(), linkTarget, linkSource);
        target.setVersion(source.getVersion());

        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        mapProperties(source, target);
        getPropertyTransformer().applyDtoPropertiesToEntity(source.getAttributes(), target);

        context.put(classKey, target);

        return target;

    }

    // CustomPropertiesDto->CustomProperties
    public static CustomProperties transformDto2CustomProperties(DtoToEntityContext tcontext,
            CustomPropertiesDto source) {

        Key<UUID> key = source.getId()
                              .equals(Key.NIL_UUID.uuidValue()) ? Key.newUuid()
                                      : Key.uuidFrom(source.getId());// when the object is new we
                                                                     // need to generate a key as
                                                                     // the type is not unique

        ClassKey<Key<UUID>> classKey = new ClassKey<>(CustomProperties.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        CustomProperties target = (CustomProperties) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new SimpleProperties(key);
        target.setVersion(source.getVersion());
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        context.put(classKey, target);

        getPropertyTransformer().applyDtoPropertiesToEntity(source.getAttributes(), target);
        return target;

    }

    private static void mapProperties(BaseModelObjectDto source, ModelObject target,
            Key<UUID> key) {
        target.setId(key);
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
    }

    private static void mapProperties(NameAbleDto source, NameAble target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static <TIn, TOut> Set<TOut> map(Set<TIn> input, Function<TIn, TOut> mapper) {
        return input.stream()
                    .map(mapper)
                    .collect(Collectors.toSet());
    }

    private static PropertyTransformer getPropertyTransformer() {
        return new PropertyTransformer();
    }

    private DtoToEntityTransformer() {
    }
}
