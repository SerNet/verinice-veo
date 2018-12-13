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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.veo.model.Element;

/**
 * Context for importing a VNA file
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class ImportContext {

    /**
     * A map of ext-ids from the imported VNA file and associated UUID from the
     * elements in the database
     */
    private Map<String, Element> extIdElementMap;

    private Properties missingMappingProperties;

    public ImportContext() {
        super();
        extIdElementMap = new HashMap<>();
        missingMappingProperties = new Properties();
    }

    public void addElement(ElementImportContext elementContext) {
        if (elementContext.getElement() != null) {
            extIdElementMap.put(elementContext.getSyncObject().getExtId(),
                    elementContext.getElement());
        }
    }

    public Element getElement(String extId) {
        return extIdElementMap.get(extId);
    }

    public Properties getMissingMappingProperties() {
        return missingMappingProperties;
    }

    public void addMissingMappingProperty(String key) {
        getMissingMappingProperties().put(key, "");
    }

    public void addAllMissingMappingProperties(Properties missingMappingProperties) {
        getMissingMappingProperties().putAll(missingMappingProperties);
    }

    public void setMissingMappingProperties(Properties missingMappingProperties) {
        this.missingMappingProperties = missingMappingProperties;
    }
}
