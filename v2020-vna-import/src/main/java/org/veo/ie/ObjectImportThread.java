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
package org.veo.ie;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.sernet.sync.data.SyncAttribute;
import de.sernet.sync.data.SyncObject;
import de.sernet.sync.mapping.SyncMapping.MapObjectType;
import de.sernet.sync.mapping.SyncMapping.MapObjectType.MapAttributeType;
import java.util.Date;
import org.veo.model.Element;
import org.veo.model.ElementProperty;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class ObjectImportThread implements Callable<ObjectImportContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectImportThread.class);

    private ObjectImportContext context;

    @Autowired
    private ImportElementService nodeService;

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
        Element node = NodeFactory.newInstance(mapObject.getIntId());
        node.setTitle(TitleAdapter.getTitle(syncObject, mapObject));
        node.setParent(context.getParent());
        importProperties(syncObject.getSyncAttribute(), mapObject, node);
        nodeService.create(node);
        context.setNode(node);
    }

    private void importProperties(List<SyncAttribute> syncObjectList, MapObjectType mapObject, Element node) {
        for (SyncAttribute syncAttribute : syncObjectList) {
            if (isProperty(syncAttribute)) {
                String name = syncAttribute.getName();
                MapAttributeType mapAttribute = getMapAttribute(mapObject, name);
                if (mapAttribute != null) {
                    node.addProperty(createProperty(syncAttribute, mapAttribute));
                    
                } else {
                    LOG.warn("MapAttributeType not found, name: " + name);
                }
            }
        }
    }

    private ElementProperty createProperty(SyncAttribute syncAttribute, MapAttributeType mapAttribute) {
        List<String> valueList = syncAttribute.getValue();
        ElementProperty property = new ElementProperty();
        property.setId(mapAttribute.getIntId());
        Object value = convertValue(valueList);
        if(value instanceof Date) {
            property.setDate((Date) value);
        }
        if(value instanceof String) {
            String stringValue = (String) value;
            if(stringValue.length()>255) {
                property.setText(stringValue);
            } else {
                property.setLabel(stringValue);
            }
        }    
        if(value instanceof Integer) {
            property.setNumber((long) value);
        }
           
        if(value instanceof Long) {
            property.setNumber((long) value);
        }
        return property;
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
