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
import java.util.stream.Collectors;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.NameAble;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.transform.ClassKey;
import org.veo.core.entity.transform.TransformEntityToTargetMethod;
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
import org.veo.persistence.entity.jpa.groups.PersonGroupData;
import org.veo.persistence.entity.jpa.groups.ProcessGroupData;

/**
 * A collection of transform functions to transform entities to Data back and
 * forth.
 */
public final class DataEntityToTargetTransformer {

    // EntityLayerSupertype ->
    // EntityLayerSupertypeData
    public static EntityLayerSupertypeData transformEntityLayerSupertype2Data(
            DataEntityToTargetContext tcontext, EntityLayerSupertype source) {
        if (source instanceof Person) {
            return transformPerson2Data(tcontext, (Person) source);
        }
        if (source instanceof Asset) {
            return transformAsset2Data(tcontext, (Asset) source);
        }
        if (source instanceof Process) {
            return transformProcess2Data(tcontext, (Process) source);
        }
        if (source instanceof Document) {
            return transformDocument2Data(tcontext, (Document) source);
        }
        if (source instanceof Control) {
            return transformControl2Data(tcontext, (Control) source);
        }
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
    }

    public static PersonGroupData transformPersonGroup2Data(DataEntityToTargetContext tcontext,
            PersonGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(PersonGroupData.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        PersonGroupData target = (PersonGroupData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonGroupData();
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
                                .map(e -> transformPerson2Data(tcontext, e))
                                .collect(Collectors.toSet()));
        return target;
    }

    // Person -> PersonData
    public static PersonData transformPerson2Data(DataEntityToTargetContext tcontext,
            Person source) {
        if (source instanceof ModelGroup<?>) {
            return transformPersonGroup2Data(tcontext, (PersonGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(PersonData.class, key);
        PersonData target = (PersonData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), tcontext, tcontext.getPersonDomainsFunction()));

        target.setLinks(map(source.getLinks(), tcontext, tcontext.getPersonLinksFunction()));

        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getPersonCustomAspectsFunction()));

        target.setOwner(map(source.getOwner(), tcontext, tcontext.getPersonOwnerFunction()));

        return target;
    }

    public static AssetGroupData transformAssetGroup2Data(DataEntityToTargetContext tcontext,
            AssetGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(AssetGroupData.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        AssetGroupData target = (AssetGroupData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetGroupData();
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
                                .map(e -> transformAsset2Data(tcontext, e))
                                .collect(Collectors.toSet()));
        return target;
    }

    // Asset -> AssetData
    public static AssetData transformAsset2Data(DataEntityToTargetContext tcontext, Asset source) {
        if (source instanceof ModelGroup<?>) {
            return transformAssetGroup2Data(tcontext, (AssetGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(AssetData.class, key);
        AssetData target = (AssetData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), tcontext, tcontext.getAssetDomainsFunction()));

        target.setLinks(map(source.getLinks(), tcontext, tcontext.getAssetLinksFunction()));

        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getAssetCustomAspectsFunction()));

        target.setOwner(map(source.getOwner(), tcontext, tcontext.getAssetOwnerFunction()));

        return target;
    }

    public static ProcessGroupData transformProcessGroup2Data(DataEntityToTargetContext tcontext,
            ProcessGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(ProcessGroupData.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ProcessGroupData target = (ProcessGroupData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessGroupData();
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
                                .map(e -> transformProcess2Data(tcontext, e))
                                .collect(Collectors.toSet()));
        return target;
    }

    // Process -> ProcessData
    public static ProcessData transformProcess2Data(DataEntityToTargetContext tcontext,
            Process source) {
        if (source instanceof ModelGroup<?>) {
            return transformProcessGroup2Data(tcontext, (ProcessGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(ProcessData.class, key);
        ProcessData target = (ProcessData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), tcontext, tcontext.getProcessDomainsFunction()));

        target.setLinks(map(source.getLinks(), tcontext, tcontext.getProcessLinksFunction()));

        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getProcessCustomAspectsFunction()));

        target.setOwner(map(source.getOwner(), tcontext, tcontext.getProcessOwnerFunction()));

        return target;
    }

    public static DocumentGroupData transformDocumentGroup2Data(DataEntityToTargetContext tcontext,
            DocumentGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(DocumentGroupData.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        DocumentGroupData target = (DocumentGroupData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentGroupData();
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
                                .map(e -> transformDocument2Data(tcontext, e))
                                .collect(Collectors.toSet()));
        return target;
    }

    // Document -> DocumentData
    public static DocumentData transformDocument2Data(DataEntityToTargetContext tcontext,
            Document source) {
        if (source instanceof ModelGroup<?>) {
            return transformDocumentGroup2Data(tcontext, (DocumentGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(DocumentData.class, key);
        DocumentData target = (DocumentData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), tcontext,
                              tcontext.getDocumentDomainsFunction()));

        target.setLinks(map(source.getLinks(), tcontext, tcontext.getDocumentLinksFunction()));

        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getDocumentCustomAspectsFunction()));

        target.setOwner(map(source.getOwner(), tcontext, tcontext.getDocumentOwnerFunction()));

        return target;
    }

    public static ControlGroupData transformControlGroup2Data(DataEntityToTargetContext tcontext,
            ControlGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(ControlGroupData.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ControlGroupData target = (ControlGroupData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlGroupData();
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
                                .map(e -> transformControl2Data(tcontext, e))
                                .collect(Collectors.toSet()));
        return target;
    }

    // Control -> ControlData
    public static ControlData transformControl2Data(DataEntityToTargetContext tcontext,
            Control source) {
        if (source instanceof ModelGroup<?>) {
            return transformControlGroup2Data(tcontext, (ControlGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(ControlData.class, key);
        ControlData target = (ControlData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), tcontext, tcontext.getControlDomainsFunction()));

        target.setLinks(map(source.getLinks(), tcontext, tcontext.getControlLinksFunction()));

        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    tcontext.getControlCustomAspectsFunction()));

        target.setOwner(map(source.getOwner(), tcontext, tcontext.getControlOwnerFunction()));

        return target;
    }

    // Client -> ClientData
    public static ClientData transformClient2Data(DataEntityToTargetContext tcontext,
            Client source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(ClientData.class, key);
        ClientData target = (ClientData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ClientData();
        mapProperties(source, target, key);
        target.setName(source.getName());
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setUnits(map(source.getUnits(), tcontext, tcontext.getClientUnitsFunction()));

        target.setDomains(map(source.getDomains(), tcontext, tcontext.getClientDomainsFunction()));

        return target;
    }

    // Domain -> DomainData
    public static DomainData transformDomain2Data(DataEntityToTargetContext tcontext,
            Domain source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(DomainData.class, key);
        DomainData target = (DomainData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DomainData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        target.setActive(source.isActive());
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        return target;
    }

    // Unit -> UnitData
    public static UnitData transformUnit2Data(DataEntityToTargetContext tcontext, Unit source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(UnitData.class, key);
        UnitData target = (UnitData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new UnitData();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setUnits(map(source.getUnits(), tcontext, tcontext.getUnitUnitsFunction()));

        target.setDomains(map(source.getDomains(), tcontext, tcontext.getUnitDomainsFunction()));

        target.setClient(map(source.getClient(), tcontext, tcontext.getUnitClientFunction()));
        target.setParent(map(source.getParent(), tcontext, tcontext.getUnitParentFunction()));

        return target;
    }

    // CustomLink ->
    // CustomLinkData
    public static CustomLinkData transformCustomLink2Data(DataEntityToTargetContext tcontext,
            CustomLink source) {

        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<String>(CustomLinkData.class, key);
        CustomLinkData target = (CustomLinkData) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new CustomLinkData();
        mapProperties(source, target, key);
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDataProperties(PropertyDataMapper.getPropertyDataSet(source));

        target.setTarget(map(source.getTarget(), tcontext, tcontext.getCustomLinkTargetFunction()));
        target.setSource(map(source.getSource(), tcontext, tcontext.getCustomLinkSourceFunction()));

        return target;

    }

    // CustomProperties ->
    // CustomPropertiesData
    public static CustomPropertiesData transformCustomProperties2Data(
            DataEntityToTargetContext tcontext, CustomProperties source) {

        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<String>(CustomPropertiesData.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();

        CustomPropertiesData target = (CustomPropertiesData) context.get(classKey);

        if (target != null) {
            return target;
        }

        target = new CustomPropertiesData();
        mapProperties(source, target, key);
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        context.put(classKey, target);

        target.setDataProperties(PropertyDataMapper.getPropertyDataSet(source));
        return target;

    }

    private static void mapProperties(ModelObject source, BaseModelObjectData target, String key) {
        target.setId(key);
        target.setVersion(source.getVersion());
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
    }

    private static void mapProperties(NameAble source, NameAbleData target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static <TIn extends ModelObject, TOut> Set<TOut> map(Set<TIn> source,
            DataEntityToTargetContext tContext,
            TransformEntityToTargetMethod<TIn, TOut, DataEntityToTargetContext> mapper) {
        if (mapper != null) {
            return source.stream()
                         .map(e -> mapper.map(tContext, e))
                         .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    private static <TIn extends ModelObject, TOut> TOut map(TIn source,
            DataEntityToTargetContext tContext,
            TransformEntityToTargetMethod<TIn, TOut, DataEntityToTargetContext> mapper) {
        if (source != null && mapper != null) {
            return mapper.map(tContext, source);
        }
        return null;
    }

    private DataEntityToTargetTransformer() {
    }
}
