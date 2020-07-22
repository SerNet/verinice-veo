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
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.ClassKey;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformEntityToTargetMethod;

public class EntityToDtoContext implements TransformEntityToTargetContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static EntityToDtoContext getCompleteTransformationContext() {
        return new EntityToDtoContext();
    }

    private Map<ClassKey<String>, Object> context = new HashMap<>();

    public Map<ClassKey<String>, Object> getContext() {
        return context;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialPerson() {
        personDomains = null;
        personLinks = null;
        personCustomAspects = null;
        personOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> personDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getPersonDomainsFunction() {
        return personDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> personLinks = EntityToDtoTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> getPersonLinksFunction() {
        return personLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> personCustomAspects = EntityToDtoTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> getPersonCustomAspectsFunction() {
        return personCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> personOwner = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getPersonOwnerFunction() {
        return personOwner;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialAsset() {
        assetDomains = null;
        assetLinks = null;
        assetCustomAspects = null;
        assetOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> assetDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getAssetDomainsFunction() {
        return assetDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> assetLinks = EntityToDtoTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> getAssetLinksFunction() {
        return assetLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> assetCustomAspects = EntityToDtoTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> getAssetCustomAspectsFunction() {
        return assetCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> assetOwner = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getAssetOwnerFunction() {
        return assetOwner;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialProcess() {
        processDomains = null;
        processLinks = null;
        processCustomAspects = null;
        processOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> processDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getProcessDomainsFunction() {
        return processDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> processLinks = EntityToDtoTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> getProcessLinksFunction() {
        return processLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> processCustomAspects = EntityToDtoTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> getProcessCustomAspectsFunction() {
        return processCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> processOwner = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getProcessOwnerFunction() {
        return processOwner;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialDocument() {
        documentDomains = null;
        documentLinks = null;
        documentCustomAspects = null;
        documentOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> documentDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getDocumentDomainsFunction() {
        return documentDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> documentLinks = EntityToDtoTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> getDocumentLinksFunction() {
        return documentLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> documentCustomAspects = EntityToDtoTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> getDocumentCustomAspectsFunction() {
        return documentCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> documentOwner = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getDocumentOwnerFunction() {
        return documentOwner;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialControl() {
        controlDomains = null;
        controlLinks = null;
        controlCustomAspects = null;
        controlOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> controlDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getControlDomainsFunction() {
        return controlDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> controlLinks = EntityToDtoTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, EntityToDtoContext> getControlLinksFunction() {
        return controlLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> controlCustomAspects = EntityToDtoTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, EntityToDtoContext> getControlCustomAspectsFunction() {
        return controlCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> controlOwner = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getControlOwnerFunction() {
        return controlOwner;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialClient() {
        clientUnits = null;
        clientDomains = null;

        return this;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> clientUnits = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getClientUnitsFunction() {
        return clientUnits;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> clientDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getClientDomainsFunction() {
        return clientDomains;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialDomain() {

        return this;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialUnit() {
        unitClient = null;
        unitObjects = null;
        unitUnits = null;
        unitParent = null;
        unitDomains = null;

        return this;
    }

    private TransformEntityToTargetMethod<Client, ClientDto, EntityToDtoContext> unitClient = EntityToDtoTransformer::transformClient2Dto;

    public TransformEntityToTargetMethod<Client, ClientDto, EntityToDtoContext> getUnitClientFunction() {
        return unitClient;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, EntityToDtoContext> unitObjects = EntityToDtoTransformer::transformEntityLayerSupertype2Dto;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, EntityToDtoContext> getUnitObjectsFunction() {
        return unitObjects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> unitUnits = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getUnitUnitsFunction() {
        return unitUnits;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> unitParent = EntityToDtoTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, EntityToDtoContext> getUnitParentFunction() {
        return unitParent;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> unitDomains = EntityToDtoTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, EntityToDtoContext> getUnitDomainsFunction() {
        return unitDomains;
    }

    /**
     * Transform only attributes.
     */
    public EntityToDtoContext partialCustomLink() {
        customLinkTarget = null;
        customLinkSource = null;

        return this;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, EntityToDtoContext> customLinkTarget = EntityToDtoTransformer::transformEntityLayerSupertype2Dto;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, EntityToDtoContext> getCustomLinkTargetFunction() {
        return customLinkTarget;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, EntityToDtoContext> customLinkSource = EntityToDtoTransformer::transformEntityLayerSupertype2Dto;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, EntityToDtoContext> getCustomLinkSourceFunction() {
        return customLinkSource;
    }

}
