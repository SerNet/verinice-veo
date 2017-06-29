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
package org.veo.web.bean;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.veo.model.Element;
import org.veo.model.ElementProperty;

import com.google.common.collect.Lists;

/**
 * @author urszeidler
 *
 */
@ManagedBean(name="elementEditor-model")
@SessionScoped
public class ElementEditor {
//    @Autowired
    private TreeBean tree;

    public class PropertyEditor {
        private ElementProperty elementProperty;

        public PropertyEditor(ElementProperty input) {
            this.elementProperty = input;
        }

        public boolean isBooleanSelect() {
            return false;
        }

        public String getName() {
            return elementProperty.getTypeId();
        }

        public String getKey() {
            return elementProperty.getUuid();
        }
        
        public Object getValue() {
            if(getIsText())
                return elementProperty.getText();
            else if (getisDate())
                return elementProperty.getDate();
                
            
            return elementProperty.toString();
        }

        public boolean getisURL() {
            return false;
        }

        public boolean getisDate() {
            return elementProperty.getDate()!=null;
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


        public boolean getIsText() {
            return elementProperty.getText()!=null;
        }

        public List<?> getOptionList() {
            return Collections.emptyList();
        }
    }

    public PropertyEditor buildEditor(ElementProperty input) {
        return new ElementEditor.PropertyEditor(input);
    }

    public List<PropertyEditor> getProperties() {
        Element selectedElement = tree.getSelectedElement();
        if (selectedElement == null) {
            return Collections.emptyList();
        }

        return Lists.transform(selectedElement.getProperties(),
                i -> new ElementEditor.PropertyEditor(i));
    }

    public List<?> getNoLabelPropertyList() {
        return Collections.emptyList();
    }
    
    public List<?> getLabelPropertyList() {
        return getProperties();
    }
}
