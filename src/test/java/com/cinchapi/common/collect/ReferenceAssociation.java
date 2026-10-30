/*
 * Copyright (c) 2013-2026 Cinchapi Inc.
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.cinchapi.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

/**
 * A reference implementation of the {@link Association} contract for tests to
 * compare {@link Association} with.
 * <p>
 * Every write rebuilds the containers along its path and upserts them into the
 * root, so a write into a list of n elements copies the list. That makes this
 * implementation slow for long lists, and simple enough to trust as a statement
 * of the results that {@link Association} must produce. The flattening and
 * upsert routines it uses are its own copies, so a change to
 * {@link Associations} or {@link MergeStrategies} cannot change what it
 * expects.
 * </p>
 *
 * @author Jeff Nelson
 */
abstract class ReferenceAssociation extends AbstractMap<String, Object> {

    /**
     * Return an empty {@link ReferenceAssociation} whose containers are
     * {@link LinkedHashMap LinkedHashMaps} and {@link ArrayList ArrayLists},
     * like {@link Association#of()}.
     *
     * @return the new {@link ReferenceAssociation}
     */
    static ReferenceAssociation of() {
        return new LinkedHashReferenceAssociation();
    }

    /**
     * Return a {@link ReferenceAssociation} that holds each flattened entry of
     * the {@code map}, written in flattened order, like
     * {@link Association#of(Map)} for a {@code map} that is not an
     * {@link Association}.
     *
     * @param map the data to copy
     * @return the new {@link ReferenceAssociation}
     */
    static ReferenceAssociation of(Map<String, Object> map) {
        ReferenceAssociation association = new LinkedHashReferenceAssociation();
        forEachFlattened(map, (key, value) -> association.set(key, value));
        return association;
    }

    /**
     * Pass each flattened entry of the {@code value} to the {@code consumer},
     * using the {@code key} as the prefix of each path.
     *
     * @param key the path of the {@code value}
     * @param value the value to flatten
     * @param consumer the receiver of each path and leaf value
     */
    @SuppressWarnings("unchecked")
    private static void flattenAndConsume(String key, Object value,
            BiConsumer<String, Object> consumer) {
        if(value instanceof Map) {
            ((Map<String, Object>) value).forEach((k, v) -> {
                flattenAndConsume(key + "." + k, v, consumer);
            });
        }
        else if(value instanceof Iterable) {
            Iterator<Object> it = ((Iterable<Object>) value).iterator();
            int index = 0;
            while (it.hasNext()) {
                flattenAndConsume(key + "." + index, it.next(), consumer);
                ++index;
            }
        }
        else {
            consumer.accept(key, value);
        }
    }

    /**
     * Pass each flattened entry of the {@code map} to the {@code consumer}.
     *
     * @param map the data to flatten
     * @param consumer the receiver of each path and leaf value
     */
    private static void forEachFlattened(Map<String, Object> map,
            BiConsumer<String, Object> consumer) {
        map.forEach((key, value) -> {
            flattenAndConsume(key, value, consumer);
        });
    }

    /**
     * Upsert {@code theirs} into {@code ours} with the rules of
     * {@link MergeStrategies#upsert(Object, Object)}.
     *
     * @param ours the existing value
     * @param theirs the incoming value
     * @return the upserted value, which is {@code ours} itself when both are
     *         maps
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static Object upsert(Object ours, Object theirs) {
        if(theirs instanceof Collection && ours instanceof Collection) {
            Iterator<Object> tit = ((Collection<Object>) theirs).iterator();
            Iterator<Object> oit = ((Collection<Object>) ours).iterator();
            Collection<Object> collection = theirs instanceof Set
                    ? Sets.newLinkedHashSet()
                    : Lists.newArrayList();
            while (tit.hasNext()) {
                if(oit.hasNext()) {
                    Object f = tit.next();
                    Object i = oit.next();
                    collection.add(upsert(i, f));
                }
                else {
                    break;
                }
            }
            while (oit.hasNext()) {
                collection.add(oit.next());
            }
            while (tit.hasNext()) {
                collection.add(tit.next());
            }
            return collection;
        }
        else if(theirs instanceof Map && ours instanceof Map) {
            ((Map<String, Object>) theirs).forEach((key, value) -> {
                if(value == null) {
                    ((Map<String, Object>) ours).put(key, value);
                }
                else {
                    ((Map<String, Object>) ours).merge(key, value,
                            ReferenceAssociation::upsert);
                }
            });
            return ours;
        }
        else {
            return theirs != null ? theirs : ours;
        }
    }

    /**
     * The entries, nested in containers that each write rebuilds.
     */
    private final Map<String, Object> exploded;

    /**
     * Construct a new instance whose root comes from {@link #mapSupplier()}.
     */
    protected ReferenceAssociation() {
        this.exploded = mapSupplier().get();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return exploded.entrySet();
    }

    /**
     * Return the value at the end of the {@code path}, with the rules of
     * {@link Association#fetch(String)}.
     *
     * @param path a navigable path key
     * @return the value, or {@code null} if there is none
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T fetch(String path) {
        T value = (T) exploded.get(path);
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

    @Override
    public Object get(Object key) {
        return key instanceof String ? fetch((String) key) : null;
    }

    /**
     * Merge each flattened entry of the {@code map} into this
     * {@link ReferenceAssociation}, preferring the incoming value, like
     * {@link Association#merge(Map)}.
     *
     * @param map the data to merge
     */
    public void merge(Map<String, Object> map) {
        merge(map, MergeStrategies::theirs);
    }

    /**
     * Merge each flattened entry of the {@code map} into this
     * {@link ReferenceAssociation} with the {@code strategy}, like
     * {@link Association#merge(Map, BiFunction)}.
     *
     * @param map the data to merge
     * @param strategy the function that combines a stored value with an
     *            incoming one
     */
    public void merge(Map<String, Object> map,
            BiFunction<Object, Object, Object> strategy) {
        forEachFlattened(map, (key, value) -> {
            Object stored = get(key);
            Object computed = stored == null ? value
                    : strategy.apply(stored, value);
            set(key, computed);
        });
    }

    @Override
    public Object put(String key, Object value) {
        return set(key, value);
    }

    /**
     * Write the {@code value} at the end of the {@code path}, with the rules of
     * {@link Association#set(String, Object)}.
     *
     * @param path a navigable path key
     * @param value the value to write
     * @return the value that was at the end of the {@code path} before, or
     *         {@code null}
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
                else {
                    ((Map<String, Object>) container).put(key, val);
                }
                val = container;
            }
            upsert(exploded, val);
            return prev;
        }
        else {
            return (T) exploded.put(path, value);
        }
    }

    @Override
    public String toString() {
        return exploded.toString();
    }

    /**
     * Return a {@link Supplier} for the containers of integer components.
     *
     * @return the supplier
     */
    protected abstract Supplier<Collection<Object>> collectionSupplier();

    /**
     * Return a {@link Supplier} for the root and for the containers of other
     * components.
     *
     * @return the supplier
     */
    protected abstract Supplier<Map<String, Object>> mapSupplier();

    /**
     * A {@link ReferenceAssociation} whose containers are
     * {@link LinkedHashMap LinkedHashMaps} and {@link ArrayList ArrayLists}.
     *
     * @author Jeff Nelson
     */
    private static class LinkedHashReferenceAssociation
            extends ReferenceAssociation {

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
