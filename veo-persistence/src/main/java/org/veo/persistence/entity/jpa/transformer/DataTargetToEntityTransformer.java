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
import org.veo.core.entity.ModelObject.Lifecycle;
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
import org.veo.persistence.entity.jpa.AbstractAspectData;
import org.veo.persistence.entity.jpa.AssetData;
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
import org.veo.persistence.entity.jpa.TimeRangeData;
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getPersonDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getPersonDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getPersonOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }

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
        target.setId(key);
        target.setVersion(source.getVersion());
        target.setState(Lifecycle.CREATING);
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getPersonDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getPersonDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getPersonOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }
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
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getAssetDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getAssetDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getAssetOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }

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
        target.setId(key);
        target.setVersion(source.getVersion());
        target.setState(Lifecycle.CREATING);
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getAssetDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getAssetDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getAssetOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getProcessDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getProcessDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getProcessOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }

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
        target.setId(key);
        target.setVersion(source.getVersion());
        target.setState(Lifecycle.CREATING);
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getProcessDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getProcessDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getProcessOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getDocumentDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getDocumentDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getDocumentOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }

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
        target.setId(key);
        target.setVersion(source.getVersion());
        target.setState(Lifecycle.CREATING);
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getDocumentDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getDocumentDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getDocumentOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getControlDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getControlDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getControlOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }

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
        target.setId(key);
        target.setVersion(source.getVersion());
        target.setState(Lifecycle.CREATING);
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        context.put(classKey, target);
        if (tcontext.getControlDomainsFunction() != null) {
            Set<Domain> domains = source.getDomains()
                                        .stream()
                                        .map(e -> tcontext.getControlDomainsFunction()
                                                          .map(tcontext, e))
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
            target.setOwner(tcontext.getControlOwnerFunction()
                                    .map(tcontext, source.getOwner()));
        }
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
        target.setActive(source.isActive());
        context.put(classKey, target);

        return target;
    }

    // NameAbleData->NameAble
    public static NameAble transformData2NameAble(DataTargetToEntityContext tcontext,
            NameAbleData source) {
        NameAbleData src = source;

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
        if (src instanceof DomainData) {
            return transformData2Domain(tcontext, (DomainData) src);
        }
        if (src instanceof UnitData) {
            return transformData2Unit(tcontext, (UnitData) src);
        }
        throw new IllegalArgumentException("No transform method defined for " + src.getClass()
                                                                                   .getSimpleName());
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
        target.setVersion(source.getVersion());
        // target.setValidFrom(Instant.parse(source.getValidFrom()));
        // target.setValidUntil(Instant.parse(source.getValidUntil()));
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
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
                                        .map(e -> tcontext.getUnitDomainsFunction()
                                                          .map(tcontext, e))
                                        .collect(Collectors.toSet());
            target.setDomains(domains);
        }
        if (source.getClient() != null && tcontext.getUnitClientFunction() != null) {
            target.setClient(tcontext.getUnitClientFunction()
                                     .map(tcontext, source.getClient()));
        }
        if (source.getParent() != null && tcontext.getUnitParentFunction() != null) {
            target.setParent(tcontext.getUnitParentFunction()
                                     .map(tcontext, source.getParent()));
        }

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
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
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

    // AbstractAspectData->AbstractAspect
    public static AbstractAspect transformData2AbstractAspect(DataTargetToEntityContext tcontext,
            AbstractAspectData source) {

        // TODO : implement this method 'transformData2AbstractAspect'
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());

    }

    // TimeRangeData->TimeRange
    public static TimeRange transformData2TimeRange(DataTargetToEntityContext tcontext,
            TimeRangeData source) {

        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
        // TODO : implement this method 'transformData2TimeRange'

    }

    private DataTargetToEntityTransformer() {
    }
}
