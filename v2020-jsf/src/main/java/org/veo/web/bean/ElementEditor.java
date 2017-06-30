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
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.schema.model.PropertyDefinition;
import org.veo.schema.model.PropertyDefinition.PropertyType;
import org.veo.web.bean.service.CacheService;

import com.google.common.collect.FluentIterable;

/**
 * @author urszeidler
 *
 */
@Named("elementEditor")
@SessionScoped
public class ElementEditor {
    private static final Logger logger = LoggerFactory.getLogger(ElementEditor.class.getName());

    @Inject
    private TreeBean tree;
    @Inject
    private CacheService cacheService;

    public class PropertyEditor {
        private ElementProperty elementProperty;
        private PropertyDefinition propertyDefinition;

        public PropertyEditor(ElementProperty elementProperty,
                PropertyDefinition propertyDefinition) {
            this.elementProperty = elementProperty;
            this.propertyDefinition = propertyDefinition;
        }

        public boolean isBooleanSelect() {
            return propertyDefinition.getType() == PropertyType.BOOLEAN;
        }

        public String getName() {
            return elementProperty.getTypeId();
        }

        public String getKey() {
            return elementProperty.getUuid();
        }

        public Object getValue() {
            if (logger.isDebugEnabled()) {
                logger.debug(elementProperty.getLabel() + " :" + elementProperty.getText() + ":"
                        + elementProperty.getNumber() + ":" + elementProperty.getDate());
            }

            if (isStringValue())
                return elementProperty.getText();
            else if (getisDate())
                return elementProperty.getDate();
            else if (isBooleanSelect()) {
                return elementProperty.getText();
            } else if (isNumber()) {
                return elementProperty.getNumber();
            }
            return elementProperty.toString();
        }

        public boolean getisURL() {
            return false;
        }

        public boolean getisDate() {
            return propertyDefinition.getType() == PropertyType.DATE;
        }

        public boolean isNumber() {
            return propertyDefinition.getType() == PropertyType.NUMBER;
        }

        public boolean getisEditable() {
            return true;
        }

        public boolean getisNumericSelect() {
            return false;
        }

        public boolean getisLine() {
            return propertyDefinition.getType() == PropertyType.LABEL;
        }

        public boolean isShowLabel() {
            return true;
        }

        public boolean getIsSingleSelect() {
            return false;
        }

        public boolean getIsText() {
            return propertyDefinition.getType() == PropertyType.TEXT;
        }

        public List<?> getOptionList() {
            return Collections.emptyList();
        }

        private boolean isStringValue() {
            return propertyDefinition.getType() == PropertyType.LABEL
                    || propertyDefinition.getType() == PropertyType.TEXT
                    || propertyDefinition.getType() == PropertyType.NUMBER;
        }
    }

    public PropertyEditor buildEditor(ElementProperty input,
            PropertyDefinition propertyDefinition) {
        return new ElementEditor.PropertyEditor(input, propertyDefinition);
    }

    public List<PropertyEditor> getProperties() {
        Element selectedElement = tree.getSelectedElement();
        if (selectedElement == null) {
            return Collections.emptyList();
        }

        Map<String, PropertyDefinition> map = cacheService.getPropertyDefinitionMap()
                .get(selectedElement.getTypeId());
        if (map == null)
            return Collections.emptyList();

        return FluentIterable.from(selectedElement.getProperties())
                .filter(input -> map.containsKey(input.getTypeId()))
                .transform(i -> new ElementEditor.PropertyEditor(i, map.get(i.getTypeId())))
                .toList();
    }

    public List<PropertyEditor> getNoLabelPropertyList() {
        return Collections.emptyList();
    }

    public List<PropertyEditor> getLabelPropertyList() {
        return getProperties();
    }

    public void setTree(TreeBean tree) {
        this.tree = tree;
    }
}
