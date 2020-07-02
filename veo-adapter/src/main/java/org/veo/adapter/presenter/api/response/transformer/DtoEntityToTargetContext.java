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

public class DtoEntityToTargetContext implements TransformEntityToTargetContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static DtoEntityToTargetContext getCompleteTransformationContext() {
        return new DtoEntityToTargetContext();
    }

    private Map<ClassKey<String>, Object> context = new HashMap<>();

    public Map<ClassKey<String>, Object> getContext() {
        return context;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialPerson() {
        personDomains = null;
        personLinks = null;
        personCustomAspects = null;
        personOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> personDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getPersonDomainsFunction() {
        return personDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> personLinks = DtoEntityToTargetTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> getPersonLinksFunction() {
        return personLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> personCustomAspects = DtoEntityToTargetTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> getPersonCustomAspectsFunction() {
        return personCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> personOwner = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getPersonOwnerFunction() {
        return personOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialAsset() {
        assetDomains = null;
        assetLinks = null;
        assetCustomAspects = null;
        assetOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> assetDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getAssetDomainsFunction() {
        return assetDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> assetLinks = DtoEntityToTargetTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> getAssetLinksFunction() {
        return assetLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> assetCustomAspects = DtoEntityToTargetTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> getAssetCustomAspectsFunction() {
        return assetCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> assetOwner = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getAssetOwnerFunction() {
        return assetOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialProcess() {
        processDomains = null;
        processLinks = null;
        processCustomAspects = null;
        processOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> processDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getProcessDomainsFunction() {
        return processDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> processLinks = DtoEntityToTargetTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> getProcessLinksFunction() {
        return processLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> processCustomAspects = DtoEntityToTargetTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> getProcessCustomAspectsFunction() {
        return processCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> processOwner = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getProcessOwnerFunction() {
        return processOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialDocument() {
        documentDomains = null;
        documentLinks = null;
        documentCustomAspects = null;
        documentOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> documentDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getDocumentDomainsFunction() {
        return documentDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> documentLinks = DtoEntityToTargetTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> getDocumentLinksFunction() {
        return documentLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> documentCustomAspects = DtoEntityToTargetTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> getDocumentCustomAspectsFunction() {
        return documentCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> documentOwner = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getDocumentOwnerFunction() {
        return documentOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialControl() {
        controlDomains = null;
        controlLinks = null;
        controlCustomAspects = null;
        controlOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> controlDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getControlDomainsFunction() {
        return controlDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> controlLinks = DtoEntityToTargetTransformer::transformCustomLink2Dto;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkDto, DtoEntityToTargetContext> getControlLinksFunction() {
        return controlLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> controlCustomAspects = DtoEntityToTargetTransformer::transformCustomProperties2Dto;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesDto, DtoEntityToTargetContext> getControlCustomAspectsFunction() {
        return controlCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> controlOwner = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getControlOwnerFunction() {
        return controlOwner;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialClient() {
        clientUnits = null;
        clientDomains = null;

        return this;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> clientUnits = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getClientUnitsFunction() {
        return clientUnits;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> clientDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getClientDomainsFunction() {
        return clientDomains;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialDomain() {

        return this;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialUnit() {
        unitClient = null;
        unitObjects = null;
        unitUnits = null;
        unitParent = null;
        unitDomains = null;

        return this;
    }

    private TransformEntityToTargetMethod<Client, ClientDto, DtoEntityToTargetContext> unitClient = DtoEntityToTargetTransformer::transformClient2Dto;

    public TransformEntityToTargetMethod<Client, ClientDto, DtoEntityToTargetContext> getUnitClientFunction() {
        return unitClient;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, DtoEntityToTargetContext> unitObjects = DtoEntityToTargetTransformer::transformEntityLayerSupertype2Dto;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, DtoEntityToTargetContext> getUnitObjectsFunction() {
        return unitObjects;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> unitUnits = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getUnitUnitsFunction() {
        return unitUnits;
    }

    private TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> unitParent = DtoEntityToTargetTransformer::transformUnit2Dto;

    public TransformEntityToTargetMethod<Unit, UnitDto, DtoEntityToTargetContext> getUnitParentFunction() {
        return unitParent;
    }

    private TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> unitDomains = DtoEntityToTargetTransformer::transformDomain2Dto;

    public TransformEntityToTargetMethod<Domain, DomainDto, DtoEntityToTargetContext> getUnitDomainsFunction() {
        return unitDomains;
    }

    /**
     * Transform only attributes.
     */
    public DtoEntityToTargetContext partialCustomLink() {
        customLinkTarget = null;
        customLinkSource = null;

        return this;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, DtoEntityToTargetContext> customLinkTarget = DtoEntityToTargetTransformer::transformEntityLayerSupertype2Dto;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, DtoEntityToTargetContext> getCustomLinkTargetFunction() {
        return customLinkTarget;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, DtoEntityToTargetContext> customLinkSource = DtoEntityToTargetTransformer::transformEntityLayerSupertype2Dto;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeDto, DtoEntityToTargetContext> getCustomLinkSourceFunction() {
        return customLinkSource;
    }

}
