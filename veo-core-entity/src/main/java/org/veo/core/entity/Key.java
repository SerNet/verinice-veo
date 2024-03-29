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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.ToString;

/**
 * <code>Key</code> stores one element as a key or multiple elements as a compound key. Convenience
 * methods are provided for simple keys. A key can determine if it is equal to another key. Keys are
 * used as identity fields in domain entities.
 *
 * <p>Arbitrary objects (i.e. any other entity fields) can be used as compound keys, but in most
 * cases a tuple of other key objects should be used in compound keys.
 *
 * <p>For more information on the key pattern see the description in M.Fowler's PoEAA.
 */
@ToString(onlyExplicitlyIncluded = true)
public class Key<T> {

  private static final UUID NIL_UUID_STRING =
      UUID.fromString("00000000-0000-0000-0000-000000000000");
  public static final Key<UUID> NIL_UUID = new Key<>(NIL_UUID_STRING);

  @ToString.Include private final List<T> fields;

  @SafeVarargs
  public Key(T... fields) {
    List<T> input = Stream.of(fields).toList();
    checkFieldsNotNull(input);
    this.fields = input;
  }

  /**
   * Convenience method to return a new UUID-based key.
   *
   * @return
   */
  public static Key<UUID> newUuid() {
    return new Key<>(UUID.randomUUID());
  }

  /**
   * Creates a UUID-based key from the given String representation.
   *
   * @param value
   * @return
   */
  public static Key<UUID> uuidFrom(String value) {
    return value != null ? new Key<>(UUID.fromString(value)) : null;
  }

  /**
   * Returns a set of UUID-based keys from the given String representations.
   *
   * @param values
   * @return
   */
  public static Set<Key<UUID>> uuidsFrom(Set<String> values) {
    return values.stream().map(v -> (Key.uuidFrom(v))).collect(Collectors.toSet());
  }

  /**
   * Creates a simple key based on the given object.
   *
   * @param obj
   */
  public Key(T obj) {
    if (obj == null) throw new IllegalArgumentException("Key must not be null");
    this.fields = Collections.singletonList(obj);
  }

  /**
   * Convenience method to return an undefined key. An undefined key will never be equal to another
   * key, not even another undefined key.
   *
   * @return
   */
  public static Key<UUID> undefined() {
    return NIL_UUID;
  }

  private void checkFieldsNotNull(List<T> fields) {
    if (fields == null) throw new IllegalArgumentException("Key must not be null");
    if (fields.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("An element of a key must not be null");
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields);
  }

  @Override
  public boolean equals(Object obj) {
    if (this.fields == null) return false;
    if (this.isUndefined()) return false;
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    @SuppressWarnings("unchecked")
    Key<T> other = (Key<T>) obj;
    if (other.isUndefined()) return false;
    return this.fields.equals(other.fields);
  }

  /**
   * Check if this key is undefined.
   *
   * @return
   */
  public boolean isUndefined() {
    checkFieldsNotNull(this.fields);
    return fields.stream().anyMatch(f -> f.equals(NIL_UUID_STRING));
  }

  /**
   * Return specific part of a compound key
   *
   * @param i
   * @return
   */
  public T value(int i) {
    return fields.get(i);
  }

  /**
   * Return simple value of this key. This is only possible for non-compound keys.
   *
   * @return
   */
  public T value() {
    checkSingleKey();
    return fields.get(0);
  }

  private void checkSingleKey() {
    if (fields == null || fields.isEmpty())
      throw new IllegalStateException("Cannot take single value on uninitialized key.");

    if (fields.size() > 1)
      throw new IllegalStateException("Cannot take single value on compound key");
  }

  /**
   * The String representation of the UUID value. This is only possible for single value UUID-based
   * keys.
   *
   * @return
   */
  public String uuidValue() {
    checkSingleKey();
    return uuidValue(0);
  }

  private String uuidValue(int i) {
    if (!(fields.get(i) instanceof UUID))
      throw new IllegalStateException("Cannot take UUID value on non-UUID key");
    return fields.get(i).toString();
  }
}
