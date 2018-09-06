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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

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

/**
 * A callable task to import one element and its properties from a VNA to
 * database.
 * 
 * @see VnaImport
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Component
@Scope("prototype")
public class ElementImportTask implements Callable<ElementImportContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ElementImportTask.class);

    private static final long EPOCH_40_YEARS_AGO = LocalDateTime.now().minusYears(40)
            .toInstant(ZoneOffset.UTC).toEpochMilli();
    private static final long EPOCH_20_YEARS_LATER = LocalDateTime.now().plusYears(20)
            .toInstant(ZoneOffset.UTC).toEpochMilli();

    private ElementImportContext context;

    private static final Pattern NUMBER = Pattern.compile("-?\\d+");

    @Autowired
    private ImportElementService importElementService;

    @Autowired
    @javax.annotation.Resource(name = "SchemaTypeIdMapper")
    private TypeIdMapper typeIdMapper;

    public ElementImportTask() {
        super();
    }

    public ElementImportTask(ElementImportContext importContext) {
        super();
        this.context = importContext;
    }

    /*
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public ElementImportContext call() throws Exception {
        try {
            importObject();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Import finished {}", logObject(getSyncObject()));
            }
        } catch (Exception e) {
            LOG.error("Error while importing type: " + getSyncObject().getExtObjectType(), e);
        }
        return context;
    }

    private void importObject() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Importing {}...", logObject(getSyncObject()));
        }
        String veriniceTypeId = getSyncObject().getExtObjectType();
        String veoTypeId = getVeoElementTypeId(veriniceTypeId);

        Element element = ElementFactory.newInstance(veoTypeId);
        element.setTitle(TitleAdapter.getTitle(getSyncObject(), getMapObject()));
        element.setParent(context.getParent());
        if (context.getParent() == null) {
            element.setScope(element);
        } else {
            element.setScope(context.getParent().getScope());
        }
        importProperties(getSyncObject().getSyncAttribute(), element);
        if (isImported()) {
            importElementService.create(element);
            context.setElement(element);
        }
    }

    private MapObjectType getMapObject() {
        return getMapObject(context.getMapObjectTypeList(),
                context.getSyncObject().getExtObjectType());
    }

    private SyncObject getSyncObject() {
        return context.getSyncObject();
    }

    private void importProperties(List<SyncAttribute> syncObjectList, Element element) {
        for (SyncAttribute syncAttribute : syncObjectList) {
            importProperty(element, syncAttribute);
        }
    }

    private void importProperty(Element element, SyncAttribute syncAttribute) {
        if (isImported(syncAttribute)) {
            String name = syncAttribute.getName();
            String propertyId = getVeoPropertyTypeId(name);
            element.getProperties().addAll(createProperties(syncAttribute, propertyId));
        }
    }

    private List<ElementProperty> createProperties(SyncAttribute syncAttribute,
            String propertyTypeId) {
        List<String> valueList = syncAttribute.getValue();
        List<ElementProperty> propertyList = new ArrayList<>(valueList.size());
        boolean isMulti = valueList.size() > 1;
        int i = 0;
        for (String value : valueList) {
            ElementProperty property = new ElementProperty();
            property.setKey(propertyTypeId);
            property.setIndex(i);
            setPropertyValue(property, value, isMulti);
            if (property.getValue() != null) {
                propertyList.add(property);
            } else {
                LOG.warn("Not adding property with value null, key: {}", propertyTypeId);
            }
            i++;
        }
        return propertyList;
    }

    private void setPropertyValue(ElementProperty property, String value, boolean isMulti) {
        Optional<Long> valueAsNumber = tryToParseAsNumber(value);
        if (valueAsNumber.isPresent()) {
            Long number = valueAsNumber.get();
            if (isTimestamp(number)) {
                property.setValue(
                        TimeFormatter.getIso8601FromEpochMillis(number, ZoneId.systemDefault()));
                property.setType(Property.Type.DATE);
            } else {
                property.setValue(value);
                property.setType(Property.Type.NUMBER);
            }
        } else {
            property.setValue(value);
            property.setType(Property.Type.TEXT);
        }
        property.setCardinality(
                (isMulti) ? Property.Cardinality.MULTI : Property.Cardinality.SINGLE);
    }

    private static Optional<Long> tryToParseAsNumber(String s) {
        if (s != null && !s.isEmpty() && NUMBER.matcher(s).matches()) {
            return Optional.ofNullable(Long.valueOf(s));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns true if it is sure s is a timestamp for which NOW+20 years >
     * timestamp > NOW-40 years.
     *
     * Timestamp means the difference, measured in milliseconds, between the
     * current time and midnight, January 1, 1970 UTC.
     *
     * @param s
     *            A time timestamp / epoch milli string
     * @return true if NOW+20 years < timestamp s > NOW-40 years.
     */
    private static boolean isTimestamp(Long s) {
        return (s > EPOCH_40_YEARS_AGO && s < EPOCH_20_YEARS_LATER);
    }

    private MapObjectType getMapObject(List<MapObjectType> mapObjects, String extId) {
        for (MapObjectType mapObject : mapObjects) {
            if (extId.equals(mapObject.getExtId())) {
                return mapObject;
            }
        }
        return null;
    }

    private boolean isImported() {
        String veriniceTypeId = getSyncObject().getExtObjectType();
        boolean isVeoPropertyId = (getVeoElementTypeId(veriniceTypeId) != null);
        if (!isVeoPropertyId) {
            getContext().addMissingMappingProperty(veriniceTypeId);
        }
        return isVeoPropertyId;
    }

    private boolean isImported(SyncAttribute syncAttribute) {
        return isNotEmpty(syncAttribute) && isVeoPropertyId(syncAttribute);
    }

    private boolean isVeoPropertyId(SyncAttribute syncAttribute) {
        String name = syncAttribute.getName();
        boolean isVeoPropertyId = (getVeoPropertyTypeId(name) != null);
        if (!isVeoPropertyId) {
            getContext().addMissingMappingProperty(name);
        }
        return isVeoPropertyId;
    }

    private boolean isNotEmpty(SyncAttribute syncAttribute) {
        return syncAttribute != null && syncAttribute.getName() != null
                && syncAttribute.getValue() != null;
    }

    public ElementImportContext getContext() {
        return context;
    }

    public void setContext(ElementImportContext importContext) {
        this.context = importContext;
    }

    private String logObject(SyncObject syncObject) {
        return "object: " + syncObject.getExtObjectType() + " - " + syncObject.getExtId();
    }

    private String getVeoElementTypeId(String vnaTypeId) {
        return typeIdMapper.getVeoElementTypeId(vnaTypeId);
    }

    private String getVeoPropertyTypeId(String vnaTypeId) {
        return typeIdMapper.getVeoPropertyTypeId(vnaTypeId);
    }

}
