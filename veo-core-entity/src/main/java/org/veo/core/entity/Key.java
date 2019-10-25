/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.entity;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * <code>Key</code> stores one element as a key or multiple elements as a
 * compound key. Convenience methods are provided for simple keys. A key can
 * determine if it is equal to another key. Keys are used as identity fields in
 * domain entities.
 *
 * Arbitrary objects (i.e. any other entity fields) can be used as compound
 * keys, but in most cases a tuple of other key objects should be used in
 * compound keys.
 *
 * For more information on the key pattern see the description in PoEAA.
 *
 */
public class Key {

    private enum SPECIAL_KEYS {
        KEY_UNDEFINED
    };

    private Object[] fields;

    public Key(Object... fields) {
        checkFieldsNotNull(fields);
        this.fields = fields;
    }

    public Key(UUID uuid) {
        if (uuid == null)
            throw new IllegalArgumentException("Key must not be null");
        this.fields = new Object[1];
        fields[0] = uuid;
    }

    /**
     * Convenience method to return a new UUID-based key.
     *
     * @return
     */
    public static Key newUuid() {
        return new Key(UUID.randomUUID());
    }

    public Key(Object obj) {
        if (obj == null)
            throw new IllegalArgumentException("Key must not be null");
        this.fields = new Object[1];
        this.fields[0] = obj;
    }

    private Key(SPECIAL_KEYS obj) {
        if (obj == null)
            throw new IllegalArgumentException("Key must not be null");
        this.fields = new Object[1];
        this.fields[0] = obj;
    }

    /**
     * Convenience method to return an undefined key. An undefined key will never be
     * equal to another key, not even another undefined key.
     *
     * @return
     */
    public static Key undefined() {
        return new Key(SPECIAL_KEYS.KEY_UNDEFINED);
    }

    private void checkFieldsNotNull(Object[] fields) {
        if (fields == null)
            throw new IllegalArgumentException("Key must not be null");
        if (Arrays.stream(fields).anyMatch(Objects::isNull))
            throw new IllegalArgumentException("An element of a key must not be null");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.deepHashCode(fields);
        return result;
    }

    /**
     *
     */
    @Override
    public boolean equals(Object obj) {
        if (this.fields.length == 1 && this.fields[0].equals(SPECIAL_KEYS.KEY_UNDEFINED))
            return false;
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Key other = (Key) obj;
        if (this.fields.length != other.fields.length)
            return false;
        if (other.isUndefined())
            return false;
        return Arrays.equals(this.fields, other.fields);
    }

    /**
     * Check if this key is undefined.
     *
     * @return
     */
    public boolean isUndefined() {
        return fields[0] == SPECIAL_KEYS.KEY_UNDEFINED;
    }

    /**
     * Return specific part of a compound key
     *
     * @param i
     * @return
     */
    public Object value(int i) {
        return fields[i];
    }

    /**
     * Return simple value of this key. This is only possible for non-compound keys.
     *
     * @return
     */
    public Object value() {
        checkSingleKey();
        return fields[0];
    }

    private void checkSingleKey() {
        if (fields.length > 0)
            throw new IllegalStateException("Cannot take single value on compound key");
    }

    /**
     * The String representation of the UUID value. This is only possible for single
     * value UUID-based keys.
     *
     * @return
     */
    public String uuidValue() {
        checkSingleKey();
        return uuidValue(0);
    }

    private String uuidValue(int i) {
        if (!(fields[i] instanceof UUID))
            throw new IllegalStateException("Cannot take UUID value on non-UUID key");
        return fields[i].toString();
    }

}
