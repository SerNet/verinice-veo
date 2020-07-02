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

import java.util.HashMap;
import java.util.Map;

import org.veo.core.entity.Client;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.ClassKey;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformEntityToTargetMethod;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.CustomLinkData;
import org.veo.persistence.entity.jpa.CustomPropertiesData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.UnitData;

public class DataEntityToTargetContext implements TransformEntityToTargetContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static DataEntityToTargetContext getCompleteTransformationContext() {
        return new DataEntityToTargetContext();
    }

    private Map<ClassKey<String>, Object> context = new HashMap<>();

    public Map<ClassKey<String>, Object> getContext() {
        return context;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialPerson() {
        personDomains = null;
        personLinks = null;
        personCustomAspects = null;
        personOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> personDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getPersonDomainsFunction() {
        return personDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> personLinks = DataEntityToTargetTransformer::transformCustomLink2Data;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> getPersonLinksFunction() {
        return personLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> personCustomAspects = DataEntityToTargetTransformer::transformCustomProperties2Data;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> getPersonCustomAspectsFunction() {
        return personCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> personOwner = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getPersonOwnerFunction() {
        return personOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialAsset() {
        assetDomains = null;
        assetLinks = null;
        assetCustomAspects = null;
        assetOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> assetDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getAssetDomainsFunction() {
        return assetDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> assetLinks = DataEntityToTargetTransformer::transformCustomLink2Data;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> getAssetLinksFunction() {
        return assetLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> assetCustomAspects = DataEntityToTargetTransformer::transformCustomProperties2Data;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> getAssetCustomAspectsFunction() {
        return assetCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> assetOwner = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getAssetOwnerFunction() {
        return assetOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialProcess() {
        processDomains = null;
        processLinks = null;
        processCustomAspects = null;
        processOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> processDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getProcessDomainsFunction() {
        return processDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> processLinks = DataEntityToTargetTransformer::transformCustomLink2Data;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> getProcessLinksFunction() {
        return processLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> processCustomAspects = DataEntityToTargetTransformer::transformCustomProperties2Data;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> getProcessCustomAspectsFunction() {
        return processCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> processOwner = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getProcessOwnerFunction() {
        return processOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialDocument() {
        documentDomains = null;
        documentLinks = null;
        documentCustomAspects = null;
        documentOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> documentDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getDocumentDomainsFunction() {
        return documentDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> documentLinks = DataEntityToTargetTransformer::transformCustomLink2Data;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> getDocumentLinksFunction() {
        return documentLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> documentCustomAspects = DataEntityToTargetTransformer::transformCustomProperties2Data;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> getDocumentCustomAspectsFunction() {
        return documentCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> documentOwner = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getDocumentOwnerFunction() {
        return documentOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialControl() {
        controlDomains = null;
        controlLinks = null;
        controlCustomAspects = null;
        controlOwner = null;

        return this;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> controlDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getControlDomainsFunction() {
        return controlDomains;
    }

    private TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> controlLinks = DataEntityToTargetTransformer::transformCustomLink2Data;

    public TransformEntityToTargetMethod<CustomLink, CustomLinkData, DataEntityToTargetContext> getControlLinksFunction() {
        return controlLinks;
    }

    private TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> controlCustomAspects = DataEntityToTargetTransformer::transformCustomProperties2Data;

    public TransformEntityToTargetMethod<CustomProperties, CustomPropertiesData, DataEntityToTargetContext> getControlCustomAspectsFunction() {
        return controlCustomAspects;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> controlOwner = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getControlOwnerFunction() {
        return controlOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialClient() {
        clientUnits = null;
        clientDomains = null;

        return this;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> clientUnits = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getClientUnitsFunction() {
        return clientUnits;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> clientDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getClientDomainsFunction() {
        return clientDomains;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialDomain() {

        return this;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialUnit() {
        unitClient = null;
        unitUnits = null;
        unitParent = null;
        unitDomains = null;

        return this;
    }

    private TransformEntityToTargetMethod<Client, ClientData, DataEntityToTargetContext> unitClient = DataEntityToTargetTransformer::transformClient2Data;

    public TransformEntityToTargetMethod<Client, ClientData, DataEntityToTargetContext> getUnitClientFunction() {
        return unitClient;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> unitUnits = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getUnitUnitsFunction() {
        return unitUnits;
    }

    private TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> unitParent = DataEntityToTargetTransformer::transformUnit2Data;

    public TransformEntityToTargetMethod<Unit, UnitData, DataEntityToTargetContext> getUnitParentFunction() {
        return unitParent;
    }

    private TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> unitDomains = DataEntityToTargetTransformer::transformDomain2Data;

    public TransformEntityToTargetMethod<Domain, DomainData, DataEntityToTargetContext> getUnitDomainsFunction() {
        return unitDomains;
    }

    /**
     * Transform only attributes.
     */
    public DataEntityToTargetContext partialCustomLink() {
        customLinkTarget = null;
        customLinkSource = null;

        return this;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeData, DataEntityToTargetContext> customLinkTarget = DataEntityToTargetTransformer::transformEntityLayerSupertype2Data;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeData, DataEntityToTargetContext> getCustomLinkTargetFunction() {
        return customLinkTarget;
    }

    private TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeData, DataEntityToTargetContext> customLinkSource = DataEntityToTargetTransformer::transformEntityLayerSupertype2Data;

    public TransformEntityToTargetMethod<EntityLayerSupertype, EntityLayerSupertypeData, DataEntityToTargetContext> getCustomLinkSourceFunction() {
        return customLinkSource;
    }

}
