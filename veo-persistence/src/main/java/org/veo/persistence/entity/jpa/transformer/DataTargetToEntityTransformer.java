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
package org.veo.persistence.entity.jpa.transformer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.proxy.HibernateProxy;

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
import org.veo.core.entity.transform.TransformTargetToEntityMethod;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.BaseModelObjectData;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.CustomLinkData;
import org.veo.persistence.entity.jpa.CustomPropertiesData;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.NameAbleData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.groups.AssetGroupData;
import org.veo.persistence.entity.jpa.groups.ControlGroupData;
import org.veo.persistence.entity.jpa.groups.DocumentGroupData;
import org.veo.persistence.entity.jpa.groups.EntityLayerSupertypeGroupData;
import org.veo.persistence.entity.jpa.groups.PersonGroupData;
import org.veo.persistence.entity.jpa.groups.ProcessGroupData;

/**
 * A collection of transform functions to transform entities to Data back and
 * forth.
 */
public final class DataTargetToEntityTransformer {

    // EntityLayerSupertypeData->EntityLayerSupertype
    public static EntityLayerSupertype transformData2EntityLayerSupertype(
            DataTargetToEntityContext tcontext, EntityLayerSupertypeData source) {
        EntityLayerSupertypeData src = source;

        if (src instanceof HibernateProxy) {
            // FIXME VEO-114 This will load the entire graph up to the whole database. Needs
            // to be changed.
            HibernateProxy proxy = (HibernateProxy) src;
            src = (EntityLayerSupertypeData) proxy.getHibernateLazyInitializer()
                                                  .getImplementation();
        }

        if (src instanceof PersonData) {
            return transformData2Person(tcontext, (PersonData) src);
        }
        if (src instanceof AssetData) {
            return transformData2Asset(tcontext, (AssetData) src);
        }
        if (src instanceof ProcessData) {
            return transformData2Process(tcontext, (ProcessData) src);
        }
        if (src instanceof DocumentData) {
            return transformData2Document(tcontext, (DocumentData) src);
        }
        if (src instanceof ControlData) {
            return transformData2Control(tcontext, (ControlData) src);
        }
        throw new IllegalArgumentException("No transform method defined for " + src.getClass()
                                                                                   .getSimpleName());
    }

    // PersonData->Person
    public static Person transformData2Person(DataTargetToEntityContext tcontext,
            PersonData source) {
        if (source instanceof EntityLayerSupertypeGroupData<?>) {
            return transformData2PersonGroup(tcontext, (PersonGroupData) source);
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
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getPersonDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getPersonLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getPersonCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getPersonOwnerFunction()));

        return target;
    }

    public static PersonGroup transformData2PersonGroup(DataTargetToEntityContext tcontext,
            PersonGroupData source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(PersonGroupData.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        PersonGroup target = (PersonGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getPersonDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getPersonLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getPersonCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getPersonOwnerFunction()));
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> transformData2Person(tcontext, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // AssetData->Asset
    public static Asset transformData2Asset(DataTargetToEntityContext tcontext, AssetData source) {
        if (source instanceof EntityLayerSupertypeGroupData<?>) {
            return transformData2AssetGroup(tcontext, (AssetGroupData) source);
        }
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Asset.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Asset target = (Asset) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetImpl(key, source.getName(), null);
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getAssetDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getAssetLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getAssetCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getAssetOwnerFunction()));

        return target;
    }

    public static AssetGroup transformData2AssetGroup(DataTargetToEntityContext tcontext,
            AssetGroupData source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(AssetGroupData.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        AssetGroup target = (AssetGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getAssetDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getAssetLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getAssetCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getAssetOwnerFunction()));
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> transformData2Asset(tcontext, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ProcessData->Process
    public static Process transformData2Process(DataTargetToEntityContext tcontext,
            ProcessData source) {
        if (source instanceof EntityLayerSupertypeGroupData<?>) {
            return transformData2ProcessGroup(tcontext, (ProcessGroupData) source);
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
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getProcessDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getProcessLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getProcessCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getProcessOwnerFunction()));

        return target;
    }

    public static ProcessGroup transformData2ProcessGroup(DataTargetToEntityContext tcontext,
            ProcessGroupData source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ProcessGroupData.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ProcessGroup target = (ProcessGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getProcessDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getProcessLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getProcessCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getProcessOwnerFunction()));
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> transformData2Process(tcontext, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // DocumentData->Document
    public static Document transformData2Document(DataTargetToEntityContext tcontext,
            DocumentData source) {
        if (source instanceof EntityLayerSupertypeGroupData<?>) {
            return transformData2DocumentGroup(tcontext, (DocumentGroupData) source);
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
        target.setDomains(map(source.getDomains(), tcontext,
                              tcontext.getDocumentDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getDocumentLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getDocumentCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getDocumentOwnerFunction()));

        return target;
    }

    public static DocumentGroup transformData2DocumentGroup(DataTargetToEntityContext tcontext,
            DocumentGroupData source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(DocumentGroupData.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        DocumentGroup target = (DocumentGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(), tcontext,
                              tcontext.getDocumentDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getDocumentLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getDocumentCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getDocumentOwnerFunction()));
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> transformData2Document(tcontext, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ControlData->Control
    public static Control transformData2Control(DataTargetToEntityContext tcontext,
            ControlData source) {
        if (source instanceof EntityLayerSupertypeGroupData<?>) {
            return transformData2ControlGroup(tcontext, (ControlGroupData) source);
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
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getControlDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getControlLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getControlCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getControlOwnerFunction()));

        return target;
    }

    public static ControlGroup transformData2ControlGroup(DataTargetToEntityContext tcontext,
            ControlGroupData source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(ControlGroupData.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();
        ControlGroup target = (ControlGroup) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlGroup();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getControlDomainsFunction()));
        target.setLinks(map(source.getLinks(), tcontext, tcontext.getControlLinksFunction()));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getControlCustomAspectsFunction()));
        target.setOwner(map(source.getOwner(), tcontext, tcontext.getControlOwnerFunction()));
        target.setMembers(source.getMembers()
                                .stream()
                                .map(e -> transformData2Control(tcontext, e))
                                .collect(Collectors.toSet()));

        return target;
    }

    // ClientData->Client
    public static Client transformData2Client(DataTargetToEntityContext tcontext,
            ClientData source) {
        Key<UUID> key = Key.uuidFrom(source.getId());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(Client.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        Client target = (Client) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ClientImpl(key, source.getName());
        mapProperties(source, target, key);
        context.put(classKey, target);
        if (tcontext.getClientUnitsFunction() != null) {
            Set<Unit> units = source.getUnits()
                                    .stream()
                                    .map(e -> tcontext.getClientUnitsFunction()
                                                      .map(tcontext, e))
                                    .collect(Collectors.toSet());
            target.setUnits(units);
        }
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getClientDomainsFunction()));

        return target;
    }

    // DomainData->Domain
    public static Domain transformData2Domain(DataTargetToEntityContext tcontext,
            DomainData source) {
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

    // UnitData->Unit
    public static Unit transformData2Unit(DataTargetToEntityContext tcontext, UnitData source) {
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
        if (tcontext.getUnitUnitsFunction() != null) {
            Set<Unit> units = source.getUnits()
                                    .stream()
                                    .map(e -> tcontext.getUnitUnitsFunction()
                                                      .map(tcontext, e))
                                    .collect(Collectors.toSet());
            target.setUnits(units);
        }
        target.setDomains(map(source.getDomains(), tcontext, tcontext.getUnitDomainsFunction()));
        target.setClient(map(source.getClient(), tcontext, tcontext.getUnitClientFunction()));
        target.setParent(map(source.getParent(), tcontext, tcontext.getUnitParentFunction()));

        return target;
    }

    // CustomLinkData->CustomLink
    public static CustomLink transformData2CustomLink(DataTargetToEntityContext tcontext,
            CustomLinkData source) {

        Key<UUID> key = Key.uuidFrom(source.getIdAsString());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(CustomLink.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        CustomLink target = (CustomLink) context.get(classKey);
        if (target != null) {
            return target;
        }

        EntityLayerSupertype linkSource = null;
        EntityLayerSupertype linkTarget = null;
        if (source.getTarget() != null && tcontext.getCustomLinkTargetFunction() != null) {
            linkTarget = tcontext.getCustomLinkTargetFunction()
                                 .map(tcontext, source.getTarget());
        }
        if (source.getSource() != null && tcontext.getCustomLinkSourceFunction() != null) {
            linkSource = tcontext.getCustomLinkSourceFunction()
                                 .map(tcontext, source.getSource());
        }

        target = new LinkImpl(key, source.getName(), linkTarget, linkSource);
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        mapProperties(source, target);
        // TODO: VEO-121 urs
        context.put(classKey, target);

        for (var prop : source.getDataProperties()) {
            prop.apply(target);
        }

        return target;

    }

    // CustomPropertiesData->CustomProperties
    public static CustomProperties transformData2CustomProperties(
            DataTargetToEntityContext tcontext, CustomPropertiesData source) {

        // TODO: VEO-121 urs
        Key<UUID> key = Key.uuidFrom(source.getIdAsString());
        ClassKey<Key<UUID>> classKey = new ClassKey<>(CustomProperties.class, key);
        Map<ClassKey<Key<UUID>>, ? super ModelObject> context = tcontext.getContext();

        CustomProperties target = (CustomProperties) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new SimpleProperties(key);
        target.setType(source.getType());
        target.setApplicableTo(new HashSet<String>(source.getApplicableTo()));
        context.put(classKey, target);

        for (var prop : source.getDataProperties()) {
            prop.apply(target);
        }

        return target;

    }

    private static void mapProperties(BaseModelObjectData source, ModelObject target,
            Key<UUID> key) {
        target.setId(key);
        target.setVersion(source.getVersion());
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
    }

    private static void mapProperties(NameAbleData source, NameAble target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static <TIn, TOut extends ModelObject> Set<TOut> map(Set<TIn> source,
            DataTargetToEntityContext tContext,
            TransformTargetToEntityMethod<TIn, TOut, DataTargetToEntityContext> mapper) {
        if (mapper != null) {
            return source.stream()
                         .map(e -> mapper.map(tContext, e))
                         .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    private static <TIn, TOut extends ModelObject> TOut map(TIn source,
            DataTargetToEntityContext tContext,
            TransformTargetToEntityMethod<TIn, TOut, DataTargetToEntityContext> mapper) {
        if (source != null && mapper != null) {
            return mapper.map(tContext, source);
        }
        return null;
    }

    private DataTargetToEntityTransformer() {
    }
}
