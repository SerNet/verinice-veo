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

import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class LinkImportContext implements Comparable<LinkImportContext> {

    private String startId;
    private List<String> endIdList;
    private String type;
    private String comment;

    public LinkImportContext() {
        super();
    }

    public LinkImportContext(String startId, String type) {
        super();
        this.startId = startId;
        this.type = type;
        endIdList = new LinkedList<>();
    }

    public LinkImportContext(String startId, String endId, String type) {
        super();
        this.startId = startId;
        endIdList = new LinkedList<>();
        endIdList.add(endId);
        this.type = type;
    }

    public String getStartId() {
        return startId;
    }

    public void setStartId(String startId) {
        this.startId = startId;
    }

    public List<String> getEndIdList() {
        return endIdList;
    }

    public void addEndId(String endId) {
        endIdList.add(endId);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int compareTo(LinkImportContext other) {
        int result = 0;
        if (this == other) {
            return 0;
        }
        if (other == null) {
            return -1;
        }
        if (startId == null) {
            if (other.startId != null) {
                return 1;
            }
        }
        result = startId.compareTo(other.startId);
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((startId == null) ? 0 : startId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LinkImportContext other = (LinkImportContext) obj;
        if (startId == null) {
            if (other.startId != null) {
                return false;
            }
        } else if (!startId.equals(other.startId)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}
