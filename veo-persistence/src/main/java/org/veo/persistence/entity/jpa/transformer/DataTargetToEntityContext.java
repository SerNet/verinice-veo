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
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.CustomLinkData;
import org.veo.persistence.entity.jpa.CustomPropertiesData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.UnitData;

public class DataTargetToEntityContext implements TransformTargetToEntityContext {
    /**
     * Returns a preconfigured transformationcontext to transform all elements.
     */
    public static DataTargetToEntityContext getCompleteTransformationContext() {
        return new DataTargetToEntityContext();
    }

    private Map<ClassKey<Key<UUID>>, ? super ModelObject> context = new HashMap<>();

    public Map<ClassKey<Key<UUID>>, ? super ModelObject> getContext() {
        return context;
    }

    /**
     * Add an entity to the context which will be used in the transformation for
     * matching object. An object need the corresponding Data type and the same id
     * to Match.
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
    public DataTargetToEntityContext partialPerson() {
        personDomains = null;
        personLinks = null;
        personCustomAspects = null;
        personOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> personDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getPersonDomainsFunction() {
        return personDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> personLinks = DataTargetToEntityTransformer::transformData2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> getPersonLinksFunction() {
        return personLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> personCustomAspects = DataTargetToEntityTransformer::transformData2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> getPersonCustomAspectsFunction() {
        return personCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> personOwner = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getPersonOwnerFunction() {
        return personOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialAsset() {
        assetDomains = null;
        assetLinks = null;
        assetCustomAspects = null;
        assetOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> assetDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getAssetDomainsFunction() {
        return assetDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> assetLinks = DataTargetToEntityTransformer::transformData2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> getAssetLinksFunction() {
        return assetLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> assetCustomAspects = DataTargetToEntityTransformer::transformData2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> getAssetCustomAspectsFunction() {
        return assetCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> assetOwner = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getAssetOwnerFunction() {
        return assetOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialProcess() {
        processDomains = null;
        processLinks = null;
        processCustomAspects = null;
        processOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> processDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getProcessDomainsFunction() {
        return processDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> processLinks = DataTargetToEntityTransformer::transformData2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> getProcessLinksFunction() {
        return processLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> processCustomAspects = DataTargetToEntityTransformer::transformData2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> getProcessCustomAspectsFunction() {
        return processCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> processOwner = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getProcessOwnerFunction() {
        return processOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialDocument() {
        documentDomains = null;
        documentLinks = null;
        documentCustomAspects = null;
        documentOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> documentDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getDocumentDomainsFunction() {
        return documentDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> documentLinks = DataTargetToEntityTransformer::transformData2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> getDocumentLinksFunction() {
        return documentLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> documentCustomAspects = DataTargetToEntityTransformer::transformData2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> getDocumentCustomAspectsFunction() {
        return documentCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> documentOwner = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getDocumentOwnerFunction() {
        return documentOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialControl() {
        controlDomains = null;
        controlLinks = null;
        controlCustomAspects = null;
        controlOwner = null;

        return this;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> controlDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getControlDomainsFunction() {
        return controlDomains;
    }

    private TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> controlLinks = DataTargetToEntityTransformer::transformData2CustomLink;

    public TransformTargetToEntityMethod<CustomLinkData, CustomLink, DataTargetToEntityContext> getControlLinksFunction() {
        return controlLinks;
    }

    private TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> controlCustomAspects = DataTargetToEntityTransformer::transformData2CustomProperties;

    public TransformTargetToEntityMethod<CustomPropertiesData, CustomProperties, DataTargetToEntityContext> getControlCustomAspectsFunction() {
        return controlCustomAspects;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> controlOwner = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getControlOwnerFunction() {
        return controlOwner;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialClient() {
        clientUnits = null;
        clientDomains = null;

        return this;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> clientUnits = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getClientUnitsFunction() {
        return clientUnits;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> clientDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getClientDomainsFunction() {
        return clientDomains;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialDomain() {

        return this;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialUnit() {
        unitClient = null;
        unitUnits = null;
        unitParent = null;
        unitDomains = null;

        return this;
    }

    private TransformTargetToEntityMethod<ClientData, Client, DataTargetToEntityContext> unitClient = DataTargetToEntityTransformer::transformData2Client;

    public TransformTargetToEntityMethod<ClientData, Client, DataTargetToEntityContext> getUnitClientFunction() {
        return unitClient;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> unitUnits = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getUnitUnitsFunction() {
        return unitUnits;
    }

    public DataTargetToEntityContext noUnitUnits() {
        unitUnits = null;
        return this;
    }

    private TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> unitParent = DataTargetToEntityTransformer::transformData2Unit;

    public TransformTargetToEntityMethod<UnitData, Unit, DataTargetToEntityContext> getUnitParentFunction() {
        return unitParent;
    }

    public DataTargetToEntityContext noUnitParent() {
        unitParent = null;
        return this;
    }

    private TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> unitDomains = DataTargetToEntityTransformer::transformData2Domain;

    public TransformTargetToEntityMethod<DomainData, Domain, DataTargetToEntityContext> getUnitDomainsFunction() {
        return unitDomains;
    }

    public DataTargetToEntityContext noUnitDomains() {
        unitDomains = null;
        return this;
    }

    /**
     * Transform only attributes.
     */
    public DataTargetToEntityContext partialCustomLink() {
        customLinkTarget = null;
        customLinkSource = null;

        return this;
    }

    private TransformTargetToEntityMethod<EntityLayerSupertypeData, EntityLayerSupertype, DataTargetToEntityContext> customLinkTarget = DataTargetToEntityTransformer::transformData2EntityLayerSupertype;

    public TransformTargetToEntityMethod<EntityLayerSupertypeData, EntityLayerSupertype, DataTargetToEntityContext> getCustomLinkTargetFunction() {
        return customLinkTarget;
    }

    private TransformTargetToEntityMethod<EntityLayerSupertypeData, EntityLayerSupertype, DataTargetToEntityContext> customLinkSource = DataTargetToEntityTransformer::transformData2EntityLayerSupertype;

    public TransformTargetToEntityMethod<EntityLayerSupertypeData, EntityLayerSupertype, DataTargetToEntityContext> getCustomLinkSourceFunction() {
        return customLinkSource;
    }

}
