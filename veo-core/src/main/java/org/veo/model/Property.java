/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
package org.veo.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.veo.util.time.TimeFormatter;

/**
 * Entity base class for element and link properties.
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Property implements Serializable {

    @Id
    @Column(length = 36)
    private String uuid;

    @Column(name = "property_key", nullable = false)
    private String key;

    @Column(length = 400000)
    private String value;

    @Column(name = "property_type", nullable = false)
    private Type type = Type.TEXT;

    @Column(nullable = false)
    private Cardinality cardinality = Cardinality.SINGLE;

    @Column(name = "group_index", nullable = false)
    private int index = 0;

    @SuppressWarnings("unused")
    @Column(name = "properties_order")
    private int propertiesOrder = 0;

    public Property() {
        if (this.uuid == null) {
            UUID randomUUID = java.util.UUID.randomUUID();
            uuid = randomUUID.toString();
        }
    }

    protected Property(Type type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public void setValue(String key, String value) {
        setKey(key);
        setValue(value);
    }

    /**
     * @return The values as int
     * @exception NumberFormatException
     *                if the value cannot be parsed as an integer.
     */
    public int getValueAsInt() {
        return Integer.parseInt(getValue());
    }

    /**
     * @return The values as ZonedDateTime
     * @throws DateTimeParseException
     *             if the text cannot be parsed
     */
    public ZonedDateTime getValueAsDate() {
        return TimeFormatter.getDateFromIso8601(getValue());
    }

    public Object parseValue() {
        switch (getType()) {
        case NUMBER:
            return getValueAsInt();
        case DATE:
            return getValueAsDate();
        case TEXT:
            return getValue();
        default:
            return getValue();
        }
    }

    public boolean isNumber() {
        return Type.NUMBER.equals(getType());
    }

    public boolean isText() {
        return Type.TEXT.equals(getType());
    }

    public boolean isDate() {
        return Type.DATE.equals(getType());
    }

    public boolean isSingle() {
        return Cardinality.SINGLE.equals(getCardinality());
    }

    public boolean isMulti() {
        return Cardinality.MULTI.equals(getCardinality());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
        Property other = (Property) obj;
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", getKey(), getValue());
    }

    /**
     * The data type of a property.
     */
    public enum Type {
        TEXT, NUMBER, DATE
    }

    /**
     * Determines if a property is the only value or if there are others.
     */
    public enum Cardinality {
        SINGLE, MULTI
    }
}
