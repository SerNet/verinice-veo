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

public class DtoTargetToEntityContext implements TransformTargetToEntityContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static DtoTargetToEntityContext getCompleteTransformationContext() {
        return new DtoTargetToEntityContext();
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
    public DtoTargetToEntityContext partialPerson() {
        personDomains = null;
        personLinks = null;
        personCustomAspects = null;
        personOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> personDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getPersonDomainsFunction() {
        return personDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> personLinks = DtoTargetToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> getPersonLinksFunction() {
        return personLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> personCustomAspects = DtoTargetToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> getPersonCustomAspectsFunction() {
        return personCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> personOwner = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getPersonOwnerFunction() {
        return personOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialAsset() {
        assetDomains = null;
        assetLinks = null;
        assetCustomAspects = null;
        assetOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> assetDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getAssetDomainsFunction() {
        return assetDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> assetLinks = DtoTargetToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> getAssetLinksFunction() {
        return assetLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> assetCustomAspects = DtoTargetToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> getAssetCustomAspectsFunction() {
        return assetCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> assetOwner = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getAssetOwnerFunction() {
        return assetOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialProcess() {
        processDomains = null;
        processLinks = null;
        processCustomAspects = null;
        processOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> processDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getProcessDomainsFunction() {
        return processDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> processLinks = DtoTargetToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> getProcessLinksFunction() {
        return processLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> processCustomAspects = DtoTargetToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> getProcessCustomAspectsFunction() {
        return processCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> processOwner = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getProcessOwnerFunction() {
        return processOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialDocument() {
        documentDomains = null;
        documentLinks = null;
        documentCustomAspects = null;
        documentOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> documentDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getDocumentDomainsFunction() {
        return documentDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> documentLinks = DtoTargetToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> getDocumentLinksFunction() {
        return documentLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> documentCustomAspects = DtoTargetToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> getDocumentCustomAspectsFunction() {
        return documentCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> documentOwner = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getDocumentOwnerFunction() {
        return documentOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialControl() {
        controlDomains = null;
        controlLinks = null;
        controlCustomAspects = null;
        controlOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> controlDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getControlDomainsFunction() {
        return controlDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> controlLinks = DtoTargetToEntityTransformer::transformDto2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkDto, CustomLink, DtoTargetToEntityContext> getControlLinksFunction() {
        return controlLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> controlCustomAspects = DtoTargetToEntityTransformer::transformDto2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesDto, CustomProperties, DtoTargetToEntityContext> getControlCustomAspectsFunction() {
        return controlCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> controlOwner = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getControlOwnerFunction() {
        return controlOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialClient() {
        clientUnits = null;
        clientDomains = null;

        return this;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> clientUnits = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getClientUnitsFunction() {
        return clientUnits;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> clientDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getClientDomainsFunction() {
        return clientDomains;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialDomain() {

        return this;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialUnit() {
        unitClient = null;
        unitUnits = null;
        unitParent = null;
        unitDomains = null;

        return this;
    }

    private TransformTargetToEntityMethod<ClientDto, Client, DtoTargetToEntityContext> unitClient = DtoTargetToEntityTransformer::transformDto2Client;

    public TransformTargetToEntityMethod<ClientDto, Client, DtoTargetToEntityContext> getUnitClientFunction() {
        return unitClient;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> unitUnits = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getUnitUnitsFunction() {
        return unitUnits;
    }

    private TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> unitParent = DtoTargetToEntityTransformer::transformDto2Unit;

    public TransformTargetToEntityMethod<UnitDto, Unit, DtoTargetToEntityContext> getUnitParentFunction() {
        return unitParent;
    }

    private TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> unitDomains = DtoTargetToEntityTransformer::transformDto2Domain;

    public TransformTargetToEntityMethod<DomainDto, Domain, DtoTargetToEntityContext> getUnitDomainsFunction() {
        return unitDomains;
    }

    /**
     * Transform only attributes.
     */
    public DtoTargetToEntityContext partialCustomLink() {
        customLinkTarget = null;
        customLinkSource = null;

        return this;
    }

    private TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoTargetToEntityContext> customLinkTarget = DtoTargetToEntityTransformer::transformDto2EntityLayerSupertype;

    public TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoTargetToEntityContext> getCustomLinkTargetFunction() {
        return customLinkTarget;
    }

    private TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoTargetToEntityContext> customLinkSource = DtoTargetToEntityTransformer::transformDto2EntityLayerSupertype;

    public TransformTargetToEntityMethod<EntityLayerSupertypeDto, EntityLayerSupertype, DtoTargetToEntityContext> getCustomLinkSourceFunction() {
        return customLinkSource;
    }

}
