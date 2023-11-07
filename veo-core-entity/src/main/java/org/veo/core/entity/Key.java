/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.ToString;

/**
 * <code>Key</code> stores one element as a key. Convenience methods are provided for simple keys. A
 * key can determine if it is equal to another key. Keys are used as identity fields in domain
 * entities.
 *
 * <p>For more information on the key pattern see the description in M.Fowler's PoEAA.
 */
@ToString(onlyExplicitlyIncluded = true)
public class Key<T> {

  private static final UUID NIL_UUID_STRING =
      UUID.fromString("00000000-0000-0000-0000-000000000000");
  public static final Key<UUID> NIL_UUID = new Key<>(NIL_UUID_STRING);

  @ToString.Include private final T value;

  public Key(T value) {
    checkValueNotNull(value);
    this.value = value;
  }

  /**
   * Convenience method to return a new UUID-based key.
   *
   * @return
   */
  public static Key<UUID> newUuid() {
    return new Key<>(UUID.randomUUID());
  }

  /** Creates a UUID-based key from the given String representation. */
  public static Key<UUID> uuidFrom(String value) {
    return value != null ? new Key<>(UUID.fromString(value)) : null;
  }

  /** Returns a set of UUID-based keys from the given String representations. */
  public static Set<Key<UUID>> uuidsFrom(Set<String> values) {
    return values.stream().map(v -> (Key.uuidFrom(v))).collect(Collectors.toSet());
  }

  /** Creates a simple key based on the given object. */
  public static <T> Key<T> from(T obj) {
    if (obj == null) throw new IllegalArgumentException("Key must not be null");
    return new Key<>(obj);
  }

  /**
   * Convenience method to return an undefined key. An undefined key will never be equal to another
   * key, not even another undefined key.
   */
  public static Key<UUID> undefined() {
    return NIL_UUID;
  }

  private void checkValueNotNull(T value) {
    if (value == null) throw new IllegalArgumentException("Key must not be null");
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this.value == null) return false;
    if (this.isUndefined()) return false;
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    @SuppressWarnings("unchecked")
    Key<T> other = (Key<T>) obj;
    if (other.isUndefined()) return false;
    return this.value.equals(other.value);
  }

  /** Check if this key is undefined. */
  public boolean isUndefined() {
    checkValueNotNull(value);
    return value.equals(NIL_UUID_STRING);
  }

  /** Return simple value of this key. */
  public T value() {
    return value;
  }

  /** The String representation of the UUID value. */
  public String uuidValue() {
    if (!(value instanceof UUID))
      throw new IllegalStateException("Cannot take UUID value on non-UUID key");
    return value.toString();
  }
}
