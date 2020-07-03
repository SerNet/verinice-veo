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
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.AbstractAspectDto;
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
import org.veo.adapter.presenter.api.response.TimeRangeDto;
import org.veo.adapter.presenter.api.response.UnitDto;
import org.veo.adapter.presenter.api.response.groups.AssetGroupDto;
import org.veo.adapter.presenter.api.response.groups.ControlGroupDto;
import org.veo.adapter.presenter.api.response.groups.DocumentGroupDto;
import org.veo.adapter.presenter.api.response.groups.EntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.response.groups.PersonGroupDto;
import org.veo.adapter.presenter.api.response.groups.ProcessGroupDto;
import org.veo.core.entity.AbstractAspect;
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
import org.veo.core.entity.TimeRange;
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
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class DtoTargetToEntityTransformer {

    // EntityLayerSupertypeDto->EntityLayerSupertype
    public static EntityLayerSupertype transformDto2EntityLayerSupertype(
            DtoTargetToEntityContext tcontext, EntityLayerSupertypeDto source) {
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
    public static Person transformDto2Person(DtoTargetToEntityContext tcontext, PersonDto source) {
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
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getPersonDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getPersonLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getPersonLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getPersonCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getPersonCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getPersonOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static PersonGroup transformDto2PersonGroup(DtoTargetToEntityContext tcontext,
            PersonGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(PersonGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        PersonGroup target = (PersonGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonGroup();
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getPersonDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getPersonLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getPersonLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getPersonCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getPersonCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getPersonOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // AssetDto->Asset
    public static Asset transformDto2Asset(DtoTargetToEntityContext tcontext, AssetDto source) {
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
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getAssetDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getAssetLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getAssetLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getAssetCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getAssetCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getAssetOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static AssetGroup transformDto2AssetGroup(DtoTargetToEntityContext tcontext,
            AssetGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(AssetGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        AssetGroup target = (AssetGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetGroup();
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getAssetDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getAssetLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getAssetLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getAssetCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getAssetCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getAssetOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ProcessDto->Process
    public static Process transformDto2Process(DtoTargetToEntityContext tcontext,
            ProcessDto source) {
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
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getProcessDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getProcessLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getProcessLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getProcessCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getProcessCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getProcessOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static ProcessGroup transformDto2ProcessGroup(DtoTargetToEntityContext tcontext,
            ProcessGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ProcessGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ProcessGroup target = (ProcessGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessGroup();
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getProcessDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getProcessLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getProcessLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getProcessCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getProcessCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getProcessOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // DocumentDto->Document
    public static Document transformDto2Document(DtoTargetToEntityContext tcontext,
            DocumentDto source) {
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
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getDocumentDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getDocumentLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getDocumentLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getDocumentCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getDocumentCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getDocumentOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static DocumentGroup transformDto2DocumentGroup(DtoTargetToEntityContext tcontext,
            DocumentGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(DocumentGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        DocumentGroup target = (DocumentGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentGroup();
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getDocumentDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getDocumentLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getDocumentLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getDocumentCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getDocumentCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getDocumentOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ControlDto->Control
    public static Control transformDto2Control(DtoTargetToEntityContext tcontext,
            ControlDto source) {
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
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getControlDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getControlLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getControlLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getControlCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getControlCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getControlOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }

        return target;
    }

    public static ControlGroup transformDto2ControlGroup(DtoTargetToEntityContext tcontext,
            ControlGroupDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ControlGroupDto.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ControlGroup target = (ControlGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlGroup();
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);
        if (tcontext.getControlDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (tcontext.getControlLinksFunction() != null) {
            Set<CustomLink> links = source.getLinks()
                                          .stream()
                                          .map(e -> tcontext.getControlLinksFunction()
                                                            .map(tcontext, e))
                                          .collect(Collectors.toSet());
            target.setLinks(links);
        }
        if (tcontext.getControlCustomAspectsFunction() != null) {
            Set<CustomProperties> customAspects = source.getCustomAspects()
                                                        .stream()
                                                        .map(e -> tcontext.getControlCustomAspectsFunction()
                                                                          .map(tcontext, e))
                                                        .collect(Collectors.toSet());
            target.setCustomAspects(customAspects);
        }
        if (source.getOwner() != null && tcontext.getControlOwnerFunction() != null) {
            target.setOwner(ModelObjectReference.mapToEntity(context, source.getOwner()));
        }
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> ModelObjectReference.mapToEntity(context, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ClientDto->Client
    public static Client transformDto2Client(DtoTargetToEntityContext tcontext, ClientDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Client.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Client target = (Client) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ClientImpl(key, source.getName());
        mapModelObject(source, target, key);
        target.setName(source.getName());
        context.put(classKey, target);
        if (tcontext.getClientUnitsFunction() != null) {
            Set<Unit> units = source.getUnits()
                                    .stream()
                                    .map(e -> tcontext.getClientUnitsFunction()
                                                      .map(tcontext, e))
                                    .collect(Collectors.toSet());
            target.setUnits(units);
        }
        if (tcontext.getClientDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getClientDomainsFunction()
                                                          .map(tcontext, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }

        return target;
    }

    // DomainDto->Domain
    public static Domain transformDto2Domain(DtoTargetToEntityContext tcontext, DomainDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Domain.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Domain target = (Domain) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DomainImpl(key, source.getName());
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        target.setActive(source.isActive());
        context.put(classKey, target);

        return target;
    }

    // UnitDto->Unit
    public static Unit transformDto2Unit(DtoTargetToEntityContext tcontext, UnitDto source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Unit.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Unit target = (Unit) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new UnitImpl(key, source.getName(), null);
        mapModelObject(source, target, key);
        mapNameAble(source, target);
        context.put(classKey, target);

        if (tcontext.getUnitUnitsFunction() != null) {
            Set<Unit> units = source.getUnits()
                                    .stream()
                                    .map(e -> tcontext.getUnitUnitsFunction()
                                                      .map(tcontext, e))
                                    .collect(Collectors.toSet());
            target.setUnits(units);
        }
        if (tcontext.getUnitDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> ModelObjectReference.mapToEntity(context, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (source.getClient() != null && tcontext.getUnitClientFunction() != null) {
            target.setClient(ModelObjectReference.mapToEntity(context, source.getClient()));
        }
        if (source.getParent() != null && tcontext.getUnitParentFunction() != null) {
            target.setParent(ModelObjectReference.mapToEntity(context, source.getParent()));
        }

        return target;
    }

    // CustomLinkDto->CustomLink
    public static CustomLink transformDto2CustomLink(DtoTargetToEntityContext tcontext,
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
        if (source.getTarget() != null && tcontext.getCustomLinkTargetFunction() != null) {
            linkTarget = ModelObjectReference.mapToEntity(context, source.getTarget());
        }
        if (source.getSource() != null && tcontext.getCustomLinkSourceFunction() != null) {
            linkSource = ModelObjectReference.mapToEntity(context, source.getSource());
        }

        target = new LinkImpl(key, source.getName(), linkTarget, linkSource);
        target.setVersion(source.getVersion());

        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        mapNameAble(source, target);
        getPropertyTransformer().applyDtoPropertiesToEntity(source.getAttributes(), target);

        context.put(classKey, target);

        return target;

    }

    // CustomPropertiesDto->CustomProperties
    public static CustomProperties transformDto2CustomProperties(DtoTargetToEntityContext tcontext,
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

    // AbstractAspectDto->AbstractAspect
    public static AbstractAspect transformDto2AbstractAspect(DtoTargetToEntityContext tcontext,
            AbstractAspectDto source) {

        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
        // TODO : implement this method 'transformDto2AbstractAspect'

    }

    // TimeRangeDto->TimeRange
    public static TimeRange transformDto2TimeRange(DtoTargetToEntityContext tcontext,
            TimeRangeDto source) {

        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
        // TODO : implement this method 'transformDto2TimeRange'

    }

    private static void mapModelObject(BaseModelObjectDto source, ModelObject target,
            Key<UUID> key) {
        target.setId(key);
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
    }

    private static void mapNameAble(NameAbleDto source, NameAble target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static PropertyTransformer getPropertyTransformer() {
        return new PropertyTransformer();
    }

    private DtoTargetToEntityTransformer() {
    }
}
