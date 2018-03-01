/*******************************************************************************
 * Copyright (c) 2017 Urs Zeidler.
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
 *     Urs Zeidler uz<at>sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.web.bean.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import org.veo.model.Element;
import org.veo.schema.model.PropertyDefinition;
import org.veo.schema.model.PropertyDefinition.PropertyType;
import org.veo.web.bean.TreeBean;
import org.veo.web.bean.service.CacheService;

import com.google.common.collect.FluentIterable;

/**
 * Would be used as editor bean for the schema model. Currently not used.
 * @author urszeidler
 *
 * @deprecated
 */
@ManagedBean(name = "elementEditor-schema")
@SessionScoped
@Deprecated
public class ElementEditor {

    public static class PropertyEditor {
        private PropertyDefinition elementProperty;

        public PropertyEditor(PropertyDefinition input) {
            this.elementProperty = input;
        }

        public boolean isBooleanSelect() {
            return false;
        }

        public String getName() {
            return elementProperty.getName();
        }

        public String getKey() {
            return elementProperty.getName();
        }

        public Object getValue() {
            return elementProperty.toString();
        }

        public boolean getisURL() {
            return false;
        }

        public boolean getisDate() {
            return elementProperty.getType() == PropertyType.DATE;
        }

        public boolean getisEditable() {
            return true;
        }

        public boolean getisNumericSelect() {
            return false;
        }

        public boolean getisLine() {
            return false;
        }

        public boolean isShowLabel() {
            return true;
        }

        public boolean getIsSingleSelect() {
            return false;
        }
        
        //make this a text field for now
        public boolean getIsText() {
            return true;
        }

        public List<?> getOptionList() {
            return Collections.emptyList();
        }
        
        public Integer  getSelectedOption() {
            return null;
        }
    }

    @Inject
    private CacheService cacheService;
    @Inject
    private TreeBean tree;

    public PropertyEditor buildEditor(PropertyDefinition input) {
        return new ElementEditor.PropertyEditor(input);
    }

    public List<PropertyEditor> getProperties() {
        Element selectedElement = tree.getSelectedElement();
        if (selectedElement==null || !cacheService.getDefinitionMap().containsKey(selectedElement.getTypeId())) {
            return Collections.emptyList();
        }
        
        Set<PropertyDefinition> propertyDef = cacheService.getDefinitionMap().get(selectedElement.getTypeId()).getProperties();
        return FluentIterable.from(propertyDef).transform(i -> new ElementEditor.PropertyEditor(i)).toList();
     }

    public List<?> getNoLabelPropertyList() {
        return Collections.emptyList();
    }

    public List<?> getLabelPropertyList() {
        return getProperties();
    }

    public TreeBean getTree() {
        return tree;
    }

    public void setTree(TreeBean tree) {
        this.tree = tree;
    }
}
