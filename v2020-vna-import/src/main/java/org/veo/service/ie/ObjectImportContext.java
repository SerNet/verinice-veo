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

import java.util.List;

import org.veo.model.Element;

import de.sernet.sync.data.SyncObject;
import de.sernet.sync.mapping.SyncMapping.MapObjectType;

/**
 * A context to import one element and its properties from a VNA to database.
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class ObjectImportContext {

    private SyncObject syncObject;
    private List<MapObjectType> mapObjectTypeList;
    private Element parent;
    private Element node;

    public ObjectImportContext(Element parent, SyncObject syncObject, List<MapObjectType> mapObjectTypeList) {
        super();
        this.parent = parent;
        this.syncObject = syncObject;
        this.mapObjectTypeList = mapObjectTypeList;
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

    public Element getParent() {
        return parent;
    }

    public void setParent(Element parent) {
        this.parent = parent;
    }

    public Element getNode() {
        return node;
    }

    public void setNode(Element node) {
        this.node = node;
    }

}
