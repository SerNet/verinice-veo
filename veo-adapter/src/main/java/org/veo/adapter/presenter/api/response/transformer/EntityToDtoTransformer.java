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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

/**
 * A collection of transform functions to transform entities to DTOs.
 */
public final class EntityToDtoTransformer {

    // EntityLayerSupertype ->
    // EntityLayerSupertypeDto
    public static EntityLayerSupertypeDto transformEntityLayerSupertype2Dto(
            EntityToDtoContext tcontext, EntityLayerSupertype source) {
        if (source instanceof Person) {
            return transformPerson2Dto(tcontext, (Person) source);
        }
        if (source instanceof Asset) {
            return transformAsset2Dto(tcontext, (Asset) source);
        }
        if (source instanceof Process) {
            return transformProcess2Dto(tcontext, (Process) source);
        }
        if (source instanceof Document) {
            return transformDocument2Dto(tcontext, (Document) source);
        }
        if (source instanceof Control) {
            return transformControl2Dto(tcontext, (Control) source);
        }
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
    }

    public static PersonGroupDto transformPersonGroup2Dto(EntityToDtoContext tcontext,
            PersonGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(PersonGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        PersonGroupDto target = (PersonGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonGroupDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }
        target.setMembers(map(source.getMembers(), ModelObjectReference::from));
        return target;
    }

    // Person ->
    // PersonDto
    public static PersonDto transformPerson2Dto(EntityToDtoContext tcontext, Person source) {
        if (source instanceof ModelGroup<?>) {
            return transformPersonGroup2Dto(tcontext, (PersonGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(PersonDto.class, key);
        PersonDto target = (PersonDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new PersonDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }

        return target;
    }

    public static AssetGroupDto transformAssetGroup2Dto(EntityToDtoContext tcontext,
            AssetGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(AssetGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        AssetGroupDto target = (AssetGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetGroupDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }
        target.setMembers(map(source.getMembers(), ModelObjectReference::from));
        return target;
    }

    // Asset -> AssetDto
    public static AssetDto transformAsset2Dto(EntityToDtoContext tcontext, Asset source) {
        if (source instanceof ModelGroup<?>) {
            return transformAssetGroup2Dto(tcontext, (AssetGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(AssetDto.class, key);
        AssetDto target = (AssetDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new AssetDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }

        return target;
    }

    public static ProcessGroupDto transformProcessGroup2Dto(EntityToDtoContext tcontext,
            ProcessGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(ProcessGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ProcessGroupDto target = (ProcessGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessGroupDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }
        target.setMembers(map(source.getMembers(), ModelObjectReference::from));
        return target;
    }

    // Process ->
    // ProcessDto
    public static ProcessDto transformProcess2Dto(EntityToDtoContext tcontext, Process source) {
        if (source instanceof ModelGroup<?>) {
            return transformProcessGroup2Dto(tcontext, (ProcessGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(ProcessDto.class, key);
        ProcessDto target = (ProcessDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ProcessDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }

        return target;
    }

    public static DocumentGroupDto transformDocumentGroup2Dto(EntityToDtoContext tcontext,
            DocumentGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(DocumentGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        DocumentGroupDto target = (DocumentGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentGroupDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }
        target.setMembers(map(source.getMembers(), ModelObjectReference::from));
        return target;
    }

    // Document ->
    // DocumentDto
    public static DocumentDto transformDocument2Dto(EntityToDtoContext tcontext, Document source) {
        if (source instanceof ModelGroup<?>) {
            return transformDocumentGroup2Dto(tcontext, (DocumentGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(DocumentDto.class, key);
        DocumentDto target = (DocumentDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DocumentDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }

        return target;
    }

    public static ControlGroupDto transformControlGroup2Dto(EntityToDtoContext tcontext,
            ControlGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(ControlGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ControlGroupDto target = (ControlGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlGroupDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }
        target.setMembers(map(source.getMembers(), ModelObjectReference::from));
        return target;
    }

    // Control ->
    // ControlDto
    public static ControlDto transformControl2Dto(EntityToDtoContext tcontext, Control source) {
        if (source instanceof ModelGroup<?>) {
            return transformControlGroup2Dto(tcontext, (ControlGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(ControlDto.class, key);
        ControlDto target = (ControlDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ControlDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setDomains(map(source.getDomains(), ModelObjectReference::from));
        target.setLinks(map(source.getLinks(), tcontext,
                            EntityToDtoTransformer::transformCustomLink2Dto));
        target.setCustomAspects(map(source.getCustomAspects(), tcontext,
                                    EntityToDtoTransformer::transformCustomProperties2Dto));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner()));
        }

        return target;
    }

    // Client ->
    // ClientDto
    public static ClientDto transformClient2Dto(EntityToDtoContext tcontext, Client source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(ClientDto.class, key);
        ClientDto target = (ClientDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new ClientDto();
        mapProperties(source, target, key);
        target.setName(source.getName());
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        Set<UnitDto> units = map(source.getUnits(), e -> transformUnit2Dto(tcontext, e));
        target.setUnits(units);

        target.setDomains(map(source.getDomains(), tcontext,
                              EntityToDtoTransformer::transformDomain2Dto));

        return target;
    }

    // Domain ->
    // DomainDto
    public static DomainDto transformDomain2Dto(EntityToDtoContext tcontext, Domain source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(DomainDto.class, key);
        DomainDto target = (DomainDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new DomainDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        target.setActive(source.isActive());
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        return target;
    }

    // Unit -> UnitDto
    public static UnitDto transformUnit2Dto(EntityToDtoContext tcontext, Unit source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(UnitDto.class, key);
        UnitDto target = (UnitDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new UnitDto();
        mapProperties(source, target, key);
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setUnits(map(source.getUnits(), e -> transformUnit2Dto(tcontext, e)));
        target.setDomains(map(source.getDomains(), ModelObjectReference::from));

        if (source.getClient() != null) {
            target.setClient(ModelObjectReference.from(source.getClient()));
        }
        if (source.getParent() != null) {
            target.setParent(ModelObjectReference.from(source.getParent()));
        }

        return target;
    }

    // CustomLink ->
    // CustomLinkDto
    public static CustomLinkDto transformCustomLink2Dto(EntityToDtoContext tcontext,
            CustomLink source) {

        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<String>(CustomLinkDto.class, key);
        CustomLinkDto target = (CustomLinkDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new CustomLinkDto();
        mapProperties(source, target, key);
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        mapProperties(source, target);
        context.put(classKey, target);
        if (source.isGhost()) {
            return target;
        }

        target.setAttributes(source.getAllProperties());

        if (source.getTarget() != null) {
            target.setTarget(ModelObjectReference.from(source.getTarget()));
        }
        if (source.getSource() != null) {
            target.setSource(ModelObjectReference.from(source.getSource()));
        }

        return target;

    }

    // CustomProperties ->
    // CustomPropertiesDto
    public static CustomPropertiesDto transformCustomProperties2Dto(EntityToDtoContext tcontext,
            CustomProperties source) {

        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<String>(CustomPropertiesDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();

        CustomPropertiesDto target = (CustomPropertiesDto) context.get(classKey);

        if (target != null) {
            return target;
        }

        target = new CustomPropertiesDto();
        mapProperties(source, target, key);
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        context.put(classKey, target);

        target.setAttributes(source.getAllProperties());
        return target;
    }

    private static void mapProperties(NameAble source, NameAbleDto target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static void mapProperties(ModelObject source, BaseModelObjectDto target, String key) {
        target.setId(key);
        target.setVersion(source.getVersion());
        // target.setValidFrom(source.getValidFrom().toString());
        // target.setValidUntil(source.getValidUntil().toString());
    }

    private static <TIn extends ModelObject, TOut> Set<TOut> map(Set<TIn> source,
            EntityToDtoContext tContext,
            TransformEntityToTargetMethod<TIn, TOut, EntityToDtoContext> mapper) {
        if (mapper != null) {
            return map(source, e -> mapper.map(tContext, e));
        }
        return new HashSet<>();
    }

    private static <TIn, TOut> Set<TOut> map(Set<TIn> input, Function<TIn, TOut> mapper) {
        return input.stream()
                    .map(mapper)
                    .collect(Collectors.toSet());
    }

    private EntityToDtoTransformer() {
    }
}
