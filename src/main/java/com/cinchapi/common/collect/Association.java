/*
 * Copyright (c) 2013-2018 Cinchapi Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cinchapi.common.collect;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.cinchapi.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

/**
 * An {@link Association} is a possibly nested/complex mapping from navigable
 * paths (e.g. keys that use dots to indicate traversal into sub lists or
 * objects) to values that are either other {@link Association Associations}
 * (e.g. maps), collections or flat (e.g. primitive) objects.
 * <p>
 *
 * @author Jeff Nelson
 */
@NotThreadSafe
public abstract class Association extends AbstractMap<String, Object> {

    /**
     * Ensure that the {@code map} is already an {@link Association} or create a
     * new {@link Association} that contains all of the contents in {@code map}.
     * <p>
     * Unlike the {@link #of(Map)} factory, this one isn't guaranteed to create
     * a new object that has a distinct state from the input. In particular, if
     * the input is already an {@link Association}, the value returned from this
     * factory will be the same instance. Otherwise, a new object is returned.
     * </p>
     * 
     * @param map
     * @return an {@link Association} containing all of the data in the
     *         {@code map}
     */
    public static Association ensure(Map<String, Object> map) {
        return map instanceof Association ? (Association) map : of(map);
    }

    /**
     * Return an empty {@link Association}.
     * 
     * @return the new {@link Association}
     */
    public static Association of() {
        return new LinkedHashAssociation();
    }

    /**
     * Return an {@link Association} that contains the data in the {@code map},
     * to facilitate path traversals.
     * <p>
     * NOTE: The returned {@link Association} DOES NOT read through to the
     * provided {@code map} and the state of both structures will immediately
     * diverge.
     * </p>
     * 
     * @return the new {@link Association} containing all of the data in the
     *         {@code map}
     */
    public static Association of(Map<String, Object> map) {
        LinkedHashAssociation association = new LinkedHashAssociation();
        if(map instanceof Association) {
            ((Association) association).exploded = new LinkedHashMap<>(
                    ((Association) map).exploded);
        }
        else {
            // NOTE: The provided #map cannot be directly assigned as the
            // #exploded member of the created Association because it is
            // necessary to flatten the input map and go through the #set
            // routine to ensure that any nested containers are properly
            // flattened and made mutable.
            Associations.forEachFlattened(map,
                    (key, value) -> association.set(key, value));
        }
        return association;
    }

    /**
     * The entries in this {@link Association}. Internally, the data is
     * maintained in exploded form to support efficient retrieval of partial
     * paths.
     */
    private Map<String, Object> exploded;

    /**
     * Construct a new instance.
     */
    protected Association() {
        this.exploded = mapSupplier().get();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (String path : paths()) {
            Object stored = fetch(path);
            if(stored.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return exploded.entrySet();
    }

    /**
     * Return a possibly nested value from within the {@link Association}.
     * 
     * @param path a navigable path key (e.g. foo.bar.1.baz)
     * @return the value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T fetch(String path) {
        T value = (T) exploded.get(path); // first, check to see if the path has
                                          // been directly added to the map
        if(value == null) {
            String[] components = path.split("\\.");
            Verify.thatArgument(components.length > 0, "Invalid path " + path);
            Object source = exploded;
            for (String component : components) {
                Integer index;
                if(source == null) {
                    break;
                }
                else if((index = Ints.tryParse(component)) != null) {
                    if(source instanceof Collection
                            && ((Collection<T>) source).size() > index) {
                        source = Iterables.get((Collection<T>) source, index);
                    }
                    else {
                        source = null;
                    }
                }
                else {
                    source = source instanceof Map
                            ? ((Map<String, Object>) source).get(component)
                            : null;
                }
            }
            return source != null ? (T) source : null;
        }
        else {
            return value;
        }
    }

    /**
     * Return a possibly nested value from with the {@link Associaiton}, if it
     * is present. Otherwise, return the {@code defaultValue}.
     * <p>
     * NOTE: The returned value may be {@code null} if this {@link Associaiton}
     * permits the storage of {@code null} values.
     * </p>
     * 
     * @param path a navigable path key (e.g. foo.bar.1.baz)
     * @param defaultValue
     * @return the associated value, if it exists or the {@code defaultValue}
     */
    @Nullable
    public <T> T fetchOrDefault(String path, T defaultValue) {
        T value;
        return ((value = fetch(path)) != null || containsKey(path)) ? value
                : defaultValue;
    }

    /**
     * Return a one-dimensional map where the keys in this {@link Association}
     * are flattened into paths and mapped to the values at the destination.
     * 
     * @return a "flat" version of this {@link Association} as a {@link Map}
     */
    public Map<String, Object> flatten() {
        Map<String, Object> flattened = Maps.newLinkedHashMap();
        Associations.forEachFlattened(exploded,
                (key, value) -> flattened.put(key, value));
        return flattened;
    }

    @Override
    public Object get(Object key) {
        return key instanceof String ? fetch((String) key) : null;
    }

    /**
     * Merge the contents of the {@code map} into this {@link Association} using
     * the {@link MergeStrategies#theirs() theirs} merge strategy.
     * 
     * @param map
     */
    public void merge(Map<String, Object> map) {
        merge(map, MergeStrategies::theirs);
    }

    /**
     * Merge the contents of the {@code map} into this {@link Association} using
     * the provided merge {@code strategy}.
     * 
     * @param map
     * @param strategy
     */
    public void merge(Map<String, Object> map,
            BiFunction<Object, Object, Object> strategy) {
        Associations.forEachFlattened(map, (key, value) -> {
            Object stored = get(key);
            Object computed = stored == null ? value
                    : strategy.apply(stored, value);
            set(key, computed);
        });
    }

    /**
     * Return a Set that contains all the fetchable paths in this
     * {@link Association}.
     * <p>
     * The returned {@link Set} does not "read-through" to the underlying
     * {@link Association} for subsequent changes.
     * </p>
     * 
     * @return the paths
     */
    public Set<String> paths() {
        Set<String> paths = Sets.newLinkedHashSet();
        flatten().keySet().forEach(key -> {
            String[] parts = key.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (String path : parts) {
                sb.append(path);
                paths.add(sb.toString());
                sb.append(".");
            }
        });
        return paths;
    }

    @Override
    public Object put(String key, Object value) {
        return set(key, value);
    }

    /**
     * Set {@code value} at the end of the {@code path}.
     * 
     * @param path
     * @param value
     * @return the value that was previously at the end of the {@code path} or
     *         {@code null} if there was no value there before
     */
    @SuppressWarnings("unchecked")
    public <T> T set(String path, Object value) {
        T stored = (T) exploded.get(path);
        if(stored == null) {
            T prev = fetch(path);
            String[] components = path.split("\\.");
            Verify.thatArgument(components.length > 0, "Invalid path: {}",
                    path);
            Verify.thatArgument(Ints.tryParse(components[0]) == null,
                    "The map cannot contain keys that start with a numeric component. "
                            + "Therefore '{}' is an invalid path.",
                    path);
            Stack<String> stack = new Stack<>();
            for (String component : components) {
                stack.add(component);
            }
            Object val = value;
            while (!stack.isEmpty()) {
                String key = stack.pop();
                Integer index;
                Object container = (index = Ints.tryParse(key)) != null
                        ? collectionSupplier().get()
                        : mapSupplier().get();
                if(container instanceof Collection) {
                    Collection<Object> collection = (Collection<Object>) container;
                    for (int i = 0; i < index; ++i) {
                        collection.add(null);
                    }
                    collection.add(val);
                }
                else { // container instanceof Map
                    ((Map<String, Object>) container).put(key, val);
                }
                val = container;
            }
            Map<String, Object> map = (Map<String, Object>) val; // NOTE: This
                                                                 // cast is safe
                                                                 // because, the
                                                                 // verification
                                                                 // that the
                                                                 // first
                                                                 // component
                                                                 // isn't an
                                                                 // integer
                                                                 // ensures that
                                                                 // the path
                                                                 // itself is
                                                                 // begins with
                                                                 // a map.
            MergeStrategies.upsert(exploded, map); // Upsert the #val into the
                                                   // exploded collection
            return prev;
        }
        else {
            // The path was added directly to the map, so do the same on this
            // update
            return (T) exploded.put(path, value);
        }
    }

    @Override
    public String toString() {
        return exploded.toString();
    }

    /**
     * Return a {@link Supplier} for {@link Collection} containers.
     * 
     * @return the supplier
     */
    protected abstract Supplier<Collection<Object>> collectionSupplier();

    /**
     * Return a {@link Supplier} for {@link Map} containers.
     * 
     * @return the supplier
     */
    protected abstract Supplier<Map<String, Object>> mapSupplier();

    /**
     * An {@link Association} that is based on {@link LinkedHashMap} sorting.
     *
     * @author Jeff Nelson
     */
    private static class LinkedHashAssociation extends Association {

        @Override
        protected Supplier<Collection<Object>> collectionSupplier() {
            return ArrayList::new;
        }

        @Override
        protected Supplier<Map<String, Object>> mapSupplier() {
            return LinkedHashMap::new;
        }

    }

}
