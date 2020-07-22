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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.veo.adapter.presenter.api.response.ClientDto;
import org.veo.adapter.presenter.api.response.CustomLinkDto;
import org.veo.adapter.presenter.api.response.CustomPropertiesDto;
import org.veo.adapter.presenter.api.response.DomainDto;
import org.veo.adapter.presenter.api.response.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.response.UnitDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.ClassKey;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.entity.transform.TransformTargetToEntityMethod;

public class DtoToEntityContext implements TransformTargetToEntityContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static DtoToEntityContext getCompleteTransformationContext() {
        return new DtoToEntityContext();
    }

    private Map<ClassKey<Key<UUID>>, ? super ModelObject> context = new HashMap<>();

    public Map<ClassKey<Key<UUID>>, ? super ModelObject> getContext() {
        return context;
    }

    /**
     * Add an entity to the context which will be used in the transformation for
     * matching object. An object need the corresponding Dto type and the same id to
     * Match.
     */
    @Override
    public void addEntity(ModelObject entity) {
        Class<?>[] interfaces = Stream.of(entity.getClass()
                                                .getInterfaces())
                                      .filter(ModelObject.class::isAssignableFrom)
                                      .filter(Predicate.isEqual(ModelGroup.class)
                                                       .negate())
                                      .toArray(Class[]::new);
        if (interfaces.length == 1) {
            ClassKey<Key<UUID>> classKey = new ClassKey<>(interfaces[0], entity.getId());
            context.put(classKey, entity);
        } else {
            throw new IllegalArgumentException(
                    "The given entity implements more than one interface.");
        }
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialPerson() {
        personDomains = null;
        personLinks = null;
        personCustomAspects = null;
        personOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> personDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getPersonDomainsFunction() {
        return personDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> personLinks = DtoToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> getPersonLinksFunction() {
        return personLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> personCustomAspects = DtoToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> getPersonCustomAspectsFunction() {
        return personCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> personOwner = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getPersonOwnerFunction() {
        return personOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialAsset() {
        assetDomains = null;
        assetLinks = null;
        assetCustomAspects = null;
        assetOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> assetDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getAssetDomainsFunction() {
        return assetDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> assetLinks = DtoToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> getAssetLinksFunction() {
        return assetLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> assetCustomAspects = DtoToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> getAssetCustomAspectsFunction() {
        return assetCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> assetOwner = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getAssetOwnerFunction() {
        return assetOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialProcess() {
        processDomains = null;
        processLinks = null;
        processCustomAspects = null;
        processOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> processDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getProcessDomainsFunction() {
        return processDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> processLinks = DtoToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> getProcessLinksFunction() {
        return processLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> processCustomAspects = DtoToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> getProcessCustomAspectsFunction() {
        return processCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> processOwner = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getProcessOwnerFunction() {
        return processOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialDocument() {
        documentDomains = null;
        documentLinks = null;
        documentCustomAspects = null;
        documentOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> documentDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getDocumentDomainsFunction() {
        return documentDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> documentLinks = DtoToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> getDocumentLinksFunction() {
        return documentLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> documentCustomAspects = DtoToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> getDocumentCustomAspectsFunction() {
        return documentCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> documentOwner = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getDocumentOwnerFunction() {
        return documentOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialControl() {
        controlDomains = null;
        controlLinks = null;
        controlCustomAspects = null;
        controlOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> controlDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getControlDomainsFunction() {
        return controlDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> controlLinks = DtoToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoToEntityContext> getControlLinksFunction() {
        return controlLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> controlCustomAspects = DtoToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoToEntityContext> getControlCustomAspectsFunction() {
        return controlCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> controlOwner = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getControlOwnerFunction() {
        return controlOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialClient() {
        clientUnits = null;
        clientDomains = null;

        return this;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> clientUnits = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getClientUnitsFunction() {
        return clientUnits;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> clientDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getClientDomainsFunction() {
        return clientDomains;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialDomain() {

        return this;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialUnit() {
        unitClient = null;
        unitUnits = null;
        unitParent = null;
        unitDomains = null;

        return this;
    }

    private TransformTargetToEntityMethod<ClientDto, Client, DtoToEntityContext> unitClient = DtoToEntityTransformer::transformDto2Client;

    public TransformTargetToEntityMethod<ClientDto, Client, DtoToEntityContext> getUnitClientFunction() {
        return unitClient;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> unitUnits = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getUnitUnitsFunction() {
        return unitUnits;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> unitParent = DtoToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoToEntityContext> getUnitParentFunction() {
        return unitParent;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> unitDomains = DtoToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoToEntityContext> getUnitDomainsFunction() {
        return unitDomains;
    }

    /**
     * Transform only attributes.
     */
    public DtoToEntityContext partialCustomLink() {
        customLinkTarget = null;
        customLinkSource = null;

        return this;
    }

    private TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoToEntityContext> customLinkTarget = DtoToEntityTransformer::transformDto2EntityLayerSupertype;

    public TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoToEntityContext> getCustomLinkTargetFunction() {
        return customLinkTarget;
    }

    private TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoToEntityContext> customLinkSource = DtoToEntityTransformer::transformDto2EntityLayerSupertype;

    public TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoToEntityContext> getCustomLinkSourceFunction() {
        return customLinkSource;
    }

}
