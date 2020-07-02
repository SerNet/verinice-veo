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
 ******************************************************************************/
package org.veo.ie;

import java.util.List;
import java.util.Properties;

import de.sernet.sync.data.SyncObject;
import de.sernet.sync.mapping.SyncMapping.MapObjectType;

import org.veo.core.entity.EntityLayerSupertype;

/**
 * Context for importing an element from a VNA file.
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class ElementImportContext {

    private SyncObject syncObject;
    private List<MapObjectType> mapObjectTypeList;
    private EntityLayerSupertype parent;
    private EntityLayerSupertype element;
    private Properties missingMappingProperties;

    public ElementImportContext(EntityLayerSupertype parent, SyncObject syncObject,
            List<MapObjectType> mapObjectTypeList) {
        super();
        this.parent = parent;
        this.syncObject = syncObject;
        this.mapObjectTypeList = mapObjectTypeList;
        this.missingMappingProperties = new Properties();
    }

    public SyncObject getSyncObject() {
        return syncObject;
    }

    public void setSyncObject(SyncObject syncObject) {
        this.syncObject = syncObject;
    }

    public List<MapObjectType> getMapObjectTypeList() {
        return mapObjectTypeList;
    }

    public void setMapObjectType(List<MapObjectType> mapObjectTypeList) {
        this.mapObjectTypeList = mapObjectTypeList;
    }

    public EntityLayerSupertype getParent() {
        return parent;
    }

    public void setParent(EntityLayerSupertype parent) {
        this.parent = parent;
    }

    public EntityLayerSupertype getElement() {
        return element;
    }

    public void setElement(EntityLayerSupertype element) {
        this.element = element;
    }

    public Properties getMissingMappingProperties() {
        return missingMappingProperties;
    }

    public void addMissingMappingProperty(String key) {
        getMissingMappingProperties().put(key, "");
    }

    public void setMissingMappingProperties(Properties missingMappingProperties) {
        this.missingMappingProperties = missingMappingProperties;
    }

}
