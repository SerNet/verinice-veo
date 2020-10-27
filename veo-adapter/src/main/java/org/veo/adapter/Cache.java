/*******************************************************************************
 * Copyright (c) 2020 Alexander Ben Nasrallah.
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
package org.veo.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * A simple cache based on HashMap. The core feature is orElseCreate of the
 * Result type. To access the actual value, a supplier has to be provided, which
 * will be run if no value is present for a key (or if the value is null). The
 * supplied value with then we put in the cache.
 */
public class Cache<K, V> {
    private final Map<K, V> innerCache;

    public Cache(Map<K, V> map) {
        this.innerCache = new HashMap<>(map); // A new map to ensure innerCache is mutable.
    }

    public Cache() {
        this(new HashMap<>());
    }

    public Result<K, V> get(K key) {
        return new Result<>(key, innerCache.get(key), this);
    }

    public V put(K key, V value) {
        return innerCache.put(key, value);
    }

    public boolean containsKey(K key) {
        return innerCache.containsKey(key);
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Result<K, V> {
        K key;
        V value;
        Cache<K, V> cache;

        public V orElseCreate(Supplier<V> supplier) {
            if (value != null) {
                return value;
            }
            var foo = supplier.get();
            cache.put(key, foo);
            return foo;
        }
    }
}
