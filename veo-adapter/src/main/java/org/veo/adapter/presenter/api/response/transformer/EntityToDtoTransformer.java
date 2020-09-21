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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.CustomPropertiesDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.VersionedDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullClientDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullControlGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullEntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
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
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.transform.ClassKey;

/**
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class EntityToDtoTransformer {

    public static EntityLayerSupertypeDto transform2Dto(EntityToDtoContext context,
            EntityLayerSupertype source) {
        if (source instanceof ModelGroup) {
            return transformGroup2Dto(context, (ModelGroup<?>) source);
        }
        if (source instanceof Person) {
            return transformPerson2Dto(context, (Person) source);
        }
        if (source instanceof Asset) {
            return transformAsset2Dto(context, (Asset) source);
        }
        if (source instanceof Process) {
            return transformProcess2Dto(context, (Process) source);
        }
        if (source instanceof Document) {
            return transformDocument2Dto(context, (Document) source);
        }
        if (source instanceof Control) {
            return transformControl2Dto(context, (Control) source);
        }
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
    }

    public static FullEntityLayerSupertypeGroupDto<?> transformGroup2Dto(
            EntityToDtoContext tcontext, ModelGroup<?> source) {
        if (source instanceof PersonGroup) {
            return transformPersonGroup2Dto(tcontext, (PersonGroup) source);
        }
        if (source instanceof AssetGroup) {
            return transformAssetGroup2Dto(tcontext, (AssetGroup) source);
        }
        if (source instanceof ProcessGroup) {
            return transformProcessGroup2Dto(tcontext, (ProcessGroup) source);
        }
        if (source instanceof DocumentGroup) {
            return transformDocumentGroup2Dto(tcontext, (DocumentGroup) source);
        }
        if (source instanceof ControlGroup) {
            return transformControlGroup2Dto(tcontext, (ControlGroup) source);
        }
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
    }

    public static FullPersonGroupDto transformPersonGroup2Dto(EntityToDtoContext tcontext,
            PersonGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(FullPersonGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        FullPersonGroupDto target = (FullPersonGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullPersonGroupDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }
        target.setMembers(convertReferenceSet(source.getMembers(),
                                              tcontext.getReferenceAssembler()));
        return target;
    }

    // Person ->
    // PersonDto
    public static FullPersonDto transformPerson2Dto(EntityToDtoContext tcontext, Person source) {
        if (source instanceof ModelGroup<?>) {
            return transformPersonGroup2Dto(tcontext, (PersonGroup) source);
        }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullPersonDto.class, key);
        FullPersonDto target = (FullPersonDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullPersonDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }

        return target;
    }

    public static FullAssetGroupDto transformAssetGroup2Dto(EntityToDtoContext tcontext,
            AssetGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(FullAssetGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        FullAssetGroupDto target = (FullAssetGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullAssetGroupDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }
        target.setMembers(convertReferenceSet(source.getMembers(),
                                              tcontext.getReferenceAssembler()));
        return target;
    }

    // Asset -> AssetDto
    public static FullAssetDto transformAsset2Dto(EntityToDtoContext tcontext, Asset source) {
        // if (source instanceof ModelGroup<?>) {
        // return transformAssetGroup2Dto(tcontext, (AssetGroup) source);
        // }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullAssetDto.class, key);
        FullAssetDto target = (FullAssetDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullAssetDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }

        return target;
    }

    public static FullProcessGroupDto transformProcessGroup2Dto(EntityToDtoContext tcontext,
            ProcessGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(FullProcessGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        FullProcessGroupDto target = (FullProcessGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullProcessGroupDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }
        target.setMembers(convertReferenceSet(source.getMembers(),
                                              tcontext.getReferenceAssembler()));
        return target;
    }

    // Process ->
    // ProcessDto
    public static FullProcessDto transformProcess2Dto(EntityToDtoContext tcontext, Process source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullProcessDto.class, key);
        FullProcessDto target = (FullProcessDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullProcessDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }

        return target;
    }

    public static FullDocumentGroupDto transformDocumentGroup2Dto(EntityToDtoContext tcontext,
            DocumentGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(FullDocumentGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        FullDocumentGroupDto target = (FullDocumentGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullDocumentGroupDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }
        target.setMembers(convertReferenceSet(source.getMembers(),
                                              tcontext.getReferenceAssembler()));
        return target;
    }

    // Document ->
    // DocumentDto
    public static FullDocumentDto transformDocument2Dto(EntityToDtoContext tcontext,
            Document source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullDocumentDto.class, key);
        FullDocumentDto target = (FullDocumentDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullDocumentDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }

        return target;
    }

    public static FullControlGroupDto transformControlGroup2Dto(EntityToDtoContext tcontext,
            ControlGroup source) {
        String key = source.getId()
                           .uuidValue();
        ClassKey<String> classKey = new ClassKey<>(FullControlGroupDto.class, key);
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        FullControlGroupDto target = (FullControlGroupDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullControlGroupDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }
        target.setMembers(convertReferenceSet(source.getMembers(),
                                              tcontext.getReferenceAssembler()));
        return target;
    }

    // Control ->
    // ControlDto
    public static FullControlDto transformControl2Dto(EntityToDtoContext tcontext, Control source) {
        // if (source instanceof ModelGroup<?>) {
        // return transformControlGroup2Dto(tcontext, (ControlGroup) source);
        // }
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullControlDto.class, key);
        FullControlDto target = (FullControlDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullControlDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        target.setLinks(mapLinks(source.getLinks(), tcontext));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects(), tcontext));

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(),
                                                      tcontext.getReferenceAssembler()));
        }

        return target;
    }

    // Client ->
    // ClientDto
    public static FullClientDto transformClient2Dto(EntityToDtoContext tcontext, Client source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullClientDto.class, key);
        FullClientDto target = (FullClientDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullClientDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        target.setName(source.getName());
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertSet(source.getDomains(), e -> transformDomain2Dto(tcontext, e)));

        return target;
    }

    // Domain ->
    // DomainDto
    public static FullDomainDto transformDomain2Dto(EntityToDtoContext tcontext, Domain source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullDomainDto.class, key);
        FullDomainDto target = (FullDomainDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullDomainDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        target.setActive(source.isActive());
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        return target;
    }

    // Unit -> UnitDto
    public static FullUnitDto transformUnit2Dto(EntityToDtoContext tcontext, Unit source) {
        String key = source.getId()
                           .uuidValue();
        Map<ClassKey<String>, Object> context = tcontext.getContext();
        ClassKey<String> classKey = new ClassKey<>(FullUnitDto.class, key);
        FullUnitDto target = (FullUnitDto) context.get(classKey);
        if (target != null) {
            return target;
        }

        target = new FullUnitDto();
        target.setId(key);
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        context.put(classKey, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setDomains(convertReferenceSet(source.getDomains(),
                                              tcontext.getReferenceAssembler()));
        if (source.getClient() != null) {
            target.setClient(ModelObjectReference.from(source.getClient(),
                                                       tcontext.getReferenceAssembler()));
        }
        if (source.getParent() != null) {
            target.setParent(ModelObjectReference.from(source.getParent(),
                                                       tcontext.getReferenceAssembler()));
        }

        return target;
    }

    // CustomLink ->
    // CustomLinkDto
    public static CustomLinkDto transformCustomLink2Dto(EntityToDtoContext tcontext,
            CustomLink source) {
        var target = new CustomLinkDto();
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());
        mapNameableProperties(source, target);
        // if (source.isGhost()) {
        // return target;
        // }

        target.setAttributes(source.getAllProperties());

        if (source.getTarget() != null) {
            target.setTarget(ModelObjectReference.from(source.getTarget(),
                                                       tcontext.getReferenceAssembler()));
        }
        // if (source.getSource() != null) {
        // target.setSource(ModelObjectReference.from(source.getSource()));
        // }

        return target;

    }

    // CustomProperties ->
    // CustomPropertiesDto
    public static CustomPropertiesDto transformCustomProperties2Dto(EntityToDtoContext tcontext,
            CustomProperties source) {
        var target = new CustomPropertiesDto();
        target.setType(source.getType());
        target.setApplicableTo(source.getApplicableTo());

        target.setAttributes(source.getAllProperties());
        return target;
    }

    private static void mapNameableProperties(Nameable source, NameableDto target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static void mapVersionedProperties(Versioned source, VersionedDto target) {
        // target.setValidFrom(source.getValidFrom().toString());
    }

    private static <TIn, TOut> Set<TOut> convertSet(Set<TIn> input, Function<TIn, TOut> mapper) {
        return input.stream()
                    .map(mapper)
                    .collect(Collectors.toSet());
    }

    private static <T extends ModelObject> Set<ModelObjectReference<T>> convertReferenceSet(
            Set<T> domains, ReferenceAssembler referenceAssembler) {
        return domains.stream()
                      .map(o -> ModelObjectReference.from(o, referenceAssembler))
                      .collect(Collectors.toSet());
    }

    private static Map<String, List<CustomLinkDto>> mapLinks(Set<CustomLink> links,
            EntityToDtoContext tcontext) {
        return links.stream()
                    .map(link -> transformCustomLink2Dto(tcontext, link))
                    .collect(Collectors.groupingBy(CustomLinkDto::getType));
    }

    private static Map<String, CustomPropertiesDto> mapCustomAspects(
            Set<CustomProperties> customAspects, EntityToDtoContext tcontext) {
        return customAspects.stream()
                            .map(customAspect -> transformCustomProperties2Dto(tcontext,
                                                                               customAspect))
                            .collect(Collectors.toMap(CustomPropertiesDto::getType,
                                                      Function.identity()));
    }

    private EntityToDtoTransformer() {
    }
}
