/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
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
 * 
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package org.veo.service.ie;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.model.Property;
import org.veo.util.time.TimeFormatter;

import de.sernet.sync.data.SyncAttribute;
import de.sernet.sync.data.SyncObject;
import de.sernet.sync.mapping.SyncMapping.MapObjectType;
import de.sernet.sync.mapping.SyncMapping.MapObjectType.MapAttributeType;

/**
 * A callable task to import one element and its properties from a VNA to
 * database.
 * 
 * @see VnaImport
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Component
@Scope("prototype")
public class ObjectImportThread implements Callable<ObjectImportContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectImportThread.class);

    private static final long EPOCH_40_YEARS_AGO = LocalDateTime.now().minusYears(40).toInstant(ZoneOffset.UTC).toEpochMilli();;
    private static final long EPOCH_20_YEARS_LATER = LocalDateTime.now().plusYears(20).toInstant(ZoneOffset.UTC).toEpochMilli();;

    private ObjectImportContext context;

    @Autowired
    private ImportElementService importElementService;

    public ObjectImportThread() {
        super();
    }

    public ObjectImportThread(ObjectImportContext syncObject) {
        super();
        this.context = syncObject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public ObjectImportContext call() throws Exception {
        try {
            importObject();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Import finished " + logObject(context.getSyncObject()));
            }
        } catch (Exception e) {
            LOG.error("Error while importing type: " + context.getSyncObject().getExtObjectType(), e);
        }
        return context;
    }

    private void importObject() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Importing " + logObject(context.getSyncObject()) + "...");
        }
        SyncObject syncObject = context.getSyncObject();
        MapObjectType mapObject = getMapObject(context.getMapObjectTypeList(), syncObject.getExtObjectType());
        Element element = ElementFactory.newInstance(mapObject.getIntId());
        element.setTitle(TitleAdapter.getTitle(syncObject, mapObject));
        element.setParent(context.getParent());
        if (context.getParent() == null) {
            element.setScope(element);
        } else {
            element.setScope(context.getParent().getScope());
        }
        importProperties(syncObject.getSyncAttribute(), mapObject, element);
        importElementService.create(element);
        context.setNode(element);
    }

    private void importProperties(List<SyncAttribute> syncObjectList, MapObjectType mapObject, Element element) {
        for (SyncAttribute syncAttribute : syncObjectList) {
            if (isProperty(syncAttribute)) {
                String name = syncAttribute.getName();
                MapAttributeType mapAttribute = getMapAttribute(mapObject, name);
                String propertyId = name;
                if (mapAttribute != null) {
                    propertyId = mapAttribute.getIntId();
                } else {
                    LOG.warn("MapAttributeType not found in VNA, using ext-id: {}", name);
                }
                element.getProperties().addAll(createProperties(syncAttribute, propertyId));
            }
        }
    }

    private List<ElementProperty> createProperties(SyncAttribute syncAttribute, String propertyTypeId) {
        List<String> valueList = syncAttribute.getValue();
        List<ElementProperty> propertyList = new ArrayList<>(valueList.size());
        boolean isMulti = valueList.size()>1;
        int i = 0;
        for (String value:valueList) {
            ElementProperty property = new ElementProperty();
            property.setKey(propertyTypeId);
            property.setIndex(i);
            setPropertyValue(property, value, isMulti);
            if(property.getValue()!=null) {
                propertyList.add(property);
            } else {
                LOG.warn("Not adding property with value null, key: " + propertyTypeId);
            }
            i++;
        }
        return propertyList;
    }

    private void setPropertyValue(ElementProperty property, String value, boolean isMulti) {
        if(isTimestamp(value)) {
            property.setValue(TimeFormatter.getIso8601FromEpochMillis(Long.valueOf(value), ZoneId.systemDefault()));
            property.setType(Property.Type.DATE);
        } else if(isNumber(value)){
            property.setValue(value);
            property.setType(Property.Type.NUMBER);
        } else {
            property.setValue(value);
            property.setType(Property.Type.TEXT);
        }
        property.setCardinality((isMulti) ? Property.Cardinality.MULTI : Property.Cardinality.SINGLE);
    }

    private boolean isNumber(String s) {
        try {
            Long.valueOf(s);
            return true;
        } catch (NumberFormatException e) {
            LOG.debug("Not a number string: " + s, e);
            return false;
        }


    }

    /**
     * Returns true if s is a timestamp for which
     * NOW+20 years < timestamp > NOW-40 years.
     *
     * Timestamp means the difference, measured in milliseconds, between
     * the current time and midnight, January 1, 1970 UTC.
     *
     * @param s A time timestamp / epoch milli string
     * @return true if NOW+20 years < timestamp s > NOW-40 years.
     */
    private boolean isTimestamp(String s) {
        try {
            long milliseconds = Long.valueOf(s);
            return(milliseconds>EPOCH_40_YEARS_AGO && milliseconds<EPOCH_20_YEARS_LATER);
        } catch (NumberFormatException e) {
            LOG.debug("Not an epoch milli string: " + s, e);
            return false;
        }
    }

    private MapObjectType getMapObject(List<MapObjectType> mapObjects, String extId) {
        for (MapObjectType mapObject : mapObjects) {
            if (extId.equals(mapObject.getExtId())) {
                return mapObject;
            }
        }
        return null;
    }

    private MapAttributeType getMapAttribute(MapObjectType mapObject, String extId) {
        for (MapAttributeType mapAttribute : mapObject.getMapAttributeType()) {
            if (extId.equals(mapAttribute.getExtId())) {
                return mapAttribute;
            }
        }
        return null;
    }

    private boolean isProperty(SyncAttribute syncAttribute) {
        return syncAttribute != null && syncAttribute.getName() != null && syncAttribute.getValue() != null;
    }

    private Object convertValue(List<String> valueList) {
        return (valueList.size() > 1) ? valueList : valueList.get(0);
    }

    public ObjectImportContext getContext() {
        return context;
    }

    public void setContext(ObjectImportContext importContext) {
        this.context = importContext;
    }

    private String logObject(SyncObject syncObject) {
        return "object: " + syncObject.getExtObjectType() + " - " + syncObject.getExtId();
    }

}
