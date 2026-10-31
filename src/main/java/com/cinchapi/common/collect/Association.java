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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.CheckedExceptions;
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
 * {@link #get(Object)}, {@link #put(String, Object)} and
 * {@link #remove(Object)} read a key as a path. The dots in a path separate its
 * components, and an integer component is a list position. For example,
 * {@code get("user.tags.0")} returns the first element of the {@code tags} list
 * inside the {@code user} map.
 * </p>
 * <p>
 * The entries of an {@link Association} are its top-level keys only. So
 * {@link #size()}, {@link #keySet()} and {@link #entrySet()} count {@code user}
 * once, however many paths start with it.
 * </p>
 * <p>
 * {@link #containsValue(Object)} returns {@code true} if the
 * {@link Association} stores an equal value at any depth of its maps and
 * collections.
 * </p>
 * <p>
 * A removal changes the map or list that holds the value, so that container
 * must allow the change. Removing a list position sets it to {@code null}, so
 * the other elements keep their positions. Only a {@link List} allows that, so
 * removing a position from any other collection, such as a set, throws an
 * {@link UnsupportedOperationException}.
 * </p>
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
     * <p>
     * A list position that a list in the {@code map} supplies does not count
     * toward the {@value #MAX_LIST_GAP} positions that one write may fill with
     * {@code null}, even when the element at that position is an empty
     * container that has no flattened entries.
     * </p>
     *
     * @param map the data to copy; unless it is an {@link Association}, each of
     *            its flattened entries is written, in flattened order, as
     *            {@link #set(String, Object)} would write it
     * @return the new {@link Association} containing all of the data in the
     *         {@code map}
     * @throws IllegalArgumentException if the path of a flattened entry is
     *             invalid, or its write would fill more than
     *             {@value #MAX_LIST_GAP} list positions that the {@code map}
     *             does not supply
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
            // necessary to flatten the input map and write each entry with the
            // rules of the #set routine to ensure that any nested containers
            // are properly flattened and made mutable.
            Set<Collection<Object>> created = Collections
                    .newSetFromMap(new IdentityHashMap<>());
            ((Association) association).forEachFlattenedEntry(map,
                    (key, value) -> ((Association) association).insert(key,
                            value, created));
        }
        return association;
    }

    /**
     * Return whether the {@code values}, or any {@link Map} or
     * {@link Collection} nested inside them at any depth, contain a value equal
     * to the {@code value}.
     *
     * @param values the values to search
     * @param value the value to look for; it may be {@code null}
     * @return {@code true} if an equal value is found
     */
    private static boolean holds(Collection<?> values,
            @Nullable Object value) {
        boolean held = false;
        Iterator<?> it = values.iterator();
        while (!held && it.hasNext()) {
            Object element = it.next();
            if(Objects.equals(element, value)) {
                held = true;
            }
            else if(element instanceof Map) {
                held = holds(((Map<?, ?>) element).values(), value);
            }
            else if(element instanceof Collection) {
                held = holds((Collection<?>) element, value);
            }
        }
        return held;
    }

    /**
     * Return whether the {@code value} is an {@link ArrayList} or a
     * {@link LinkedList} itself, and not a subclass or another kind of
     * {@link Collection}, so that each call to its {@link List#add(Object) add}
     * method appends the element and the list holds each element at the
     * position it was added in.
     * 
     * @param value the value to check
     * @return {@code true} if the {@code value} is a plain list
     */
    private static boolean isPlainList(@Nullable Object value) {
        return value != null && (value.getClass() == ArrayList.class
                || value.getClass() == LinkedList.class);
    }

    /**
     * Return whether the {@code value} is a {@link LinkedHashMap} or a
     * {@link HashMap} itself, and not a subclass or another kind of
     * {@link Map}, so that its {@link Map#compute(Object, BiFunction) compute},
     * {@link Map#merge(Object, Object, BiFunction) merge} and
     * {@link Map#put(Object, Object) put} methods have no effects beyond the
     * ones that the JDK defines.
     * 
     * @param value the value to check
     * @return {@code true} if the {@code value} is a plain map
     */
    private static boolean isPlainMap(@Nullable Object value) {
        return value != null && (value.getClass() == LinkedHashMap.class
                || value.getClass() == HashMap.class);
    }

    /**
     * Whether a class of {@link Association} inherits {@link #get(Object)},
     * {@link #fetch(String)}, {@link #set(String, Object)} and
     * {@link #merge(Map, BiFunction)} from {@link Association}.
     * <p>
     * Only for such a class can {@link #merge(Map)} change lists that it made
     * in place. A subclass that overrides one of those methods can depend on
     * {@link #merge(Map)} calling it for each flattened entry, and can keep a
     * reference to a container that a later entry would change.
     * </p>
     */
    private static final ClassValue<Boolean> INHERITS_ACCESSORS = new ClassValue<Boolean>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("get", Object.class)
                        .getDeclaringClass() == Association.class
                        && type.getMethod("fetch", String.class)
                                .getDeclaringClass() == Association.class
                        && type.getMethod("set", String.class, Object.class)
                                .getDeclaringClass() == Association.class
                        && type.getMethod("merge", Map.class, BiFunction.class)
                                .getDeclaringClass() == Association.class;
            }
            catch (NoSuchMethodException e) {
                throw CheckedExceptions.wrapAsRuntimeException(e);
            }
        }

    };

    /**
     * The largest number of {@code null} elements that one write may add to the
     * lists of an {@link Association} to reach the positions that the integer
     * components of its path name.
     * <p>
     * The limit keeps the size of a list in proportion to the data written into
     * it, so that a short path such as {@code a.2000000000} cannot build a list
     * of billions of elements.
     * </p>
     */
    public static final int MAX_LIST_GAP = 100;

    /**
     * The entries in this {@link Association}. Internally, the data is
     * maintained in exploded form to support efficient retrieval of partial
     * paths.
     */
    private Map<String, Object> exploded;

    /**
     * For the flattening write call in progress, the number of positions that
     * each list of its input has supplied so far, keyed by the path of the
     * list, or {@code null} when no such call is in progress.
     */
    @Nullable
    private Map<String, Integer> supplied;

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
        return holds(exploded.values(), value);
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
        if(value == null && path.indexOf('.') >= 0) {
            String[] components = path.split("\\.");
            Verify.thatArgument(components.length > 0, "Invalid path {}", path);
            return (T) navigate(components, components.length);
        }
        else {
            // A path without a dot is a top-level key, so the lookup above
            // already checked the only place its value can be.
            return value;
        }
    }

    /**
     * Return the value at the {@code path}, or the {@code defaultValue} if
     * there is none.
     * <p>
     * If the {@code path} holds {@code null}, this method also returns the
     * {@code defaultValue}.
     * </p>
     *
     * @param path a navigable path key (e.g. foo.bar.1.baz)
     * @param defaultValue the value to return if the {@code path} has no value
     * @return the value at the {@code path}, or the {@code defaultValue}
     */
    @Nullable
    public <T> T fetchOrDefault(String path, T defaultValue) {
        T value = fetch(path);
        return value != null ? value : defaultValue;
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
     * the {@link MergeStrategies#theirs(Object, Object) theirs} merge strategy.
     * <p>
     * A list position that a list in the {@code map} supplies does not count
     * toward the {@value #MAX_LIST_GAP} positions that one write may fill with
     * {@code null}, even when the element at that position is an empty
     * container that has no flattened entries.
     * </p>
     *
     * @param map the data to merge; each of its flattened entries is written,
     *            in flattened order, as {@link #set(String, Object)} would
     *            write it
     * @throws IllegalArgumentException if the path of a flattened entry is
     *             invalid, or its write would fill more than
     *             {@value #MAX_LIST_GAP} list positions that the {@code map}
     *             does not supply; the entries before it stay written
     */
    public void merge(Map<String, Object> map) {
        if(INHERITS_ACCESSORS.get(getClass())) {
            Set<Collection<Object>> created = Collections
                    .newSetFromMap(new IdentityHashMap<>());
            forEachFlattenedEntry(map, (key, value) -> {
                // Look up each entry before writing it, as #merge(Map,
                // BiFunction) does, so that the same reads happen in the same
                // order. A read can throw, and it reorders a map that keeps its
                // entries in access order.
                Object stored = get(key);
                Object computed = stored == null ? value
                        : MergeStrategies.theirs(stored, value);
                insert(key, computed, created);
            });
        }
        else {
            merge(map, MergeStrategies::theirs);
        }
    }

    /**
     * Merge the contents of the {@code map} into this {@link Association} using
     * the provided merge {@code strategy}.
     * <p>
     * A list position that a list in the {@code map} supplies does not count
     * toward the {@value #MAX_LIST_GAP} positions that one write may fill with
     * {@code null}, even when the element at that position is an empty
     * container that has no flattened entries.
     * </p>
     *
     * @param map
     * @param strategy
     * @throws IllegalArgumentException if the path of a flattened entry is
     *             invalid, or its write would fill more than
     *             {@value #MAX_LIST_GAP} list positions that the {@code map}
     *             does not supply; the entries before it stay written
     */
    public void merge(Map<String, Object> map,
            BiFunction<Object, Object, Object> strategy) {
        forEachFlattenedEntry(map, (key, value) -> {
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

    @Override
    public Object remove(Object key) {
        return key instanceof String ? detach((String) key) : null;
    }

    /**
     * Set {@code value} at the end of the {@code path}.
     * <p>
     * An integer component of the {@code path} names a position in a list. When
     * that position is past the end of the list, the write fills the positions
     * in between with {@code null}. One write can fill at most
     * {@value #MAX_LIST_GAP} positions this way. When {@link #merge(Map)} or
     * {@link #merge(Map, BiFunction)} writes an entry through this method, the
     * positions that a list in its input supplies do not count.
     * </p>
     *
     * @param path
     * @param value
     * @return the value that was previously at the end of the {@code path} or
     *         {@code null} if there was no value there before
     * @throws IllegalArgumentException if the write would fill more than
     *             {@value #MAX_LIST_GAP} positions with {@code null}, in which
     *             case this {@link Association} does not change
     */
    @SuppressWarnings("unchecked")
    public <T> T set(String path, Object value) {
        T stored = (T) exploded.get(path);
        if(stored == null) {
            T prev = fetch(path);
            String[] components = components(path);
            if(!isPlainMap(exploded)) {
                // A map that a subclass supplies can do more in put, such as
                // reject a key, so build the outer map and let its put run.
                MergeStrategies.upsert(exploded, wrap(components, 0, value));
            }
            else {
                // The first component is never an integer, so the outer
                // container that #wrap would build is a map with one entry.
                // Merging that entry into #exploded directly has the same
                // result.
                Object theirs = components.length == 1 ? value
                        : wrap(components, 1, value);
                if(theirs == null) {
                    // Map#merge throws for a null value, so store it with put,
                    // as MergeStrategies#upsert does.
                    exploded.put(components[0], theirs);
                }
                else {
                    exploded.merge(components[0], theirs,
                            MergeStrategies::upsert);
                }
            }
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
     * <p>
     * The {@link Supplier} must return a new, empty container on each call and
     * have no other effect, because an {@link Association} may call it any
     * number of times for a write and discard what it returns.
     * </p>
     * 
     * @return the supplier
     */
    protected abstract Supplier<Collection<Object>> collectionSupplier();

    /**
     * Return a {@link Supplier} for {@link Map} containers.
     * <p>
     * The {@link Supplier} must return a new, empty container on each call and
     * have no other effect, because an {@link Association} may call it any
     * number of times for a write and discard what it returns.
     * </p>
     * 
     * @return the supplier
     */
    protected abstract Supplier<Map<String, Object>> mapSupplier();

    /**
     * Split the {@code path} into its components and verify that a value can be
     * written at its end.
     * <p>
     * A write fills each position between the end of a list and the position
     * that an integer component names with {@code null}, and a component whose
     * current value is not a {@link Collection} gets a new list. A position
     * that a list in the input of the flattening write call in progress has
     * supplied does not count. The {@code path} can be written only when its
     * write fills at most {@value #MAX_LIST_GAP} other positions in total.
     * </p>
     *
     * @param path a navigable path key
     * @return the components of the {@code path}, of which there is at least
     *         one
     * @throws IllegalArgumentException if the {@code path} has no components,
     *             its first component is an integer, or its write would fill
     *             more than {@value #MAX_LIST_GAP} positions with {@code null}
     *             that the input of the call in progress does not supply
     */
    private String[] components(String path) {
        String[] components = path.split("\\.");
        Verify.thatArgument(components.length > 0, "Invalid path: {}", path);
        Verify.thatArgument(Ints.tryParse(components[0]) == null,
                "The map cannot contain keys that start with a numeric component. "
                        + "Therefore '{}' is an invalid path.",
                path);
        boolean indexed = false;
        for (int i = 1; i < components.length && !indexed; ++i) {
            indexed = Ints.tryParse(components[i]) != null;
        }
        if(indexed) {
            // Only a list position in the path can make a write pad a list with
            // nulls, so no other path can pass the limit.
            long skipped = 0;
            Object source = exploded;
            int start = 0;
            for (String component : components) {
                Integer index = Ints.tryParse(component);
                if(index != null) {
                    int size = source instanceof Collection
                            ? ((Collection<?>) source).size()
                            : 0;
                    if(index > size) {
                        // The flattening call keys each list of its input by
                        // the exact text of the path before the list's
                        // positions, so look it up by the same text.
                        int given = supplied != null
                                ? supplied.getOrDefault(
                                        path.substring(0, start - 1), 0)
                                : 0;
                        int filled = Math.max(size, given);
                        if(index > filled) {
                            skipped += index - filled;
                        }
                    }
                    source = index >= 0 && index < size
                            ? Iterables.get((Collection<?>) source, index)
                            : null;
                }
                else {
                    source = source instanceof Map
                            ? ((Map<?, ?>) source).get(component)
                            : null;
                }
                start += component.length() + 1;
            }
            Verify.thatArgument(skipped <= MAX_LIST_GAP,
                    "The path '{}' skips {} list positions, "
                            + "but a write can skip at most {}",
                    path, skipped, MAX_LIST_GAP);
        }
        return components;
    }

    /**
     * Remove the value at the {@code path} and return it.
     * <p>
     * The {@code path} splits into components the same way that
     * {@link #fetch(String)} splits it, and this method reads them in the
     * stored containers. If the last component is a list position, the position
     * is set to {@code null}, so the later elements keep their positions. The
     * containers along the {@code path} stay, even if the removal leaves one of
     * them empty.
     * </p>
     *
     * @param path a navigable path key
     * @return the removed value, or {@code null} if there was no value at the
     *         {@code path}
     * @throws IllegalArgumentException if the {@code path} has no components,
     *             such as {@code "."}
     * @throws UnsupportedOperationException if the container that holds the
     *             value does not allow the change, or if the last component is
     *             a position in a {@link Collection} that is not a {@link List}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private Object detach(String path) {
        String[] components = path.split("\\.");
        Verify.thatArgument(components.length > 0, "Invalid path {}", path);
        int end = components.length - 1;
        Object holder = navigate(components, end);
        String last = components[end];
        Integer index = Ints.tryParse(last);
        Object detached;
        if(index == null && holder instanceof Map) {
            detached = ((Map<String, Object>) holder).remove(last);
        }
        else if(index != null && holder instanceof List && index >= 0
                && index < ((List<Object>) holder).size()) {
            detached = ((List<Object>) holder).set(index, null);
        }
        else if(index != null && holder instanceof Collection && index >= 0
                && index < ((Collection<Object>) holder).size()) {
            // Only a list can set one position to null and keep every other
            // element in place.
            throw new UnsupportedOperationException(AnyStrings.format(
                    "Cannot clear position {} of a {}, which is not a list",
                    index, holder.getClass().getSimpleName()));
        }
        else {
            detached = null;
        }
        return detached;
    }

    /**
     * Pass each flattened entry of the {@code map} to the {@code consumer}.
     * <p>
     * While it runs, this {@link Association} records how many positions each
     * list in the {@code map} has supplied, including positions whose element
     * is an empty container that has no flattened entries. A write that the
     * {@code consumer} makes, directly or through {@link #set(String, Object)},
     * does not count those positions toward {@value #MAX_LIST_GAP}.
     * </p>
     *
     * @param map the data to flatten
     * @param consumer the receiver of each flattened path and leaf value
     */
    private void forEachFlattenedEntry(Map<String, Object> map,
            BiConsumer<String, Object> consumer) {
        Map<String, Integer> outer = supplied;
        Map<String, Integer> current = new HashMap<>();
        supplied = current;
        try {
            Associations.forEachFlattened(map, consumer,
                    (list, position) -> current.merge(list, position + 1,
                            Math::max));
        }
        finally {
            supplied = outer;
        }
    }

    /**
     * Write the {@code value} at the end of the {@code path} as one of the
     * writes of a call that copies a whole map into this {@link Association}.
     * <p>
     * The entries, container classes and iteration order that result, any
     * exception, and the state after an exception are the same as for
     * {@link #set(String, Object)}. The difference is that a list in
     * {@code created} changes in place instead of being replaced by a new list,
     * so a write into a list costs the same however long the list is. The
     * caller must ensure that no code outside of the call can reach a list in
     * {@code created} before the call returns. Each list that this method makes
     * and that no other code can reach is added to {@code created}.
     * </p>
     * 
     * @param path a navigable path key
     * @param value the value to write
     * @param created the lists that the call made so far
     * @throws IllegalArgumentException if the {@code path} has no components,
     *             its first component is an integer, or the write would fill
     *             more than {@value #MAX_LIST_GAP} positions with {@code null}
     *             that the input of the call does not supply
     * @throws IndexOutOfBoundsException if a component of the {@code path} is a
     *             negative index into an existing list
     */
    private void insert(String path, Object value,
            Set<Collection<Object>> created) {
        if(exploded.get(path) == null) {
            // Read the current value, as #set does, because that read rejects
            // some paths, such as a negative index into an existing list,
            // before anything changes.
            fetch(path);
            upsertPath(exploded, components(path), 0, value, created);
        }
        else {
            exploded.put(path, value);
        }
    }

    /**
     * Follow the first {@code count} {@code components} through the stored
     * containers, and return the value they lead to.
     * <p>
     * An integer component is a position in a {@link Collection}. Any other
     * component is a key in a {@link Map}. A {@code count} of zero returns the
     * outermost map.
     * </p>
     *
     * @param components the components of a path
     * @param count how many components to follow, from the first
     * @return the value the components lead to, or {@code null} if there is
     *         none
     * @throws IndexOutOfBoundsException if a component is a negative position
     *             in a {@link Collection} along the way
     */
    @Nullable
    private Object navigate(String[] components, int count) {
        Object source = exploded;
        for (int i = 0; i < count && source != null; ++i) {
            Integer index = Ints.tryParse(components[i]);
            if(index != null) {
                source = source instanceof Collection
                        && ((Collection<?>) source).size() > index
                                ? Iterables.get((Collection<?>) source, index)
                                : null;
            }
            else {
                source = source instanceof Map
                        ? ((Map<?, ?>) source).get(components[i])
                        : null;
            }
        }
        return source;
    }

    /**
     * Upsert the containers that hold the {@code value} at the end of the
     * {@code components}, from index {@code from} on, into {@code ours}.
     * <p>
     * The result has the same contents and classes as
     * {@link MergeStrategies#upsert(Object, Object)} of {@code ours} and
     * {@link #wrap(String[], int, Object) wrap(components, from, value)}, and
     * any exception is the same. A list in {@code created} along the path
     * changes in place, and any new list that no other code can reach is added
     * to {@code created}, as for {@link #insert(String, Object, Set)}.
     * </p>
     * 
     * @param ours the existing value that holds the component at {@code from},
     *            or {@code null}
     * @param components the components of the path
     * @param from the index of the component that {@code ours} holds
     * @param value the value at the end of the path
     * @param created the lists that the current call made so far
     * @return the upserted value, which is {@code ours} itself when it changed
     *         in place
     */
    @SuppressWarnings("unchecked")
    private Object upsertPath(@Nullable Object ours, String[] components,
            int from, Object value, Set<Collection<Object>> created) {
        String component = components[from];
        Integer index = Ints.tryParse(component);
        boolean isLast = from == components.length - 1;
        if(index == null && isPlainMap(ours)) {
            Map<String, Object> map = (Map<String, Object>) ours;
            if(isLast && value == null) {
                map.put(component, null);
            }
            else if(isLast) {
                map.merge(component, value, MergeStrategies::upsert);
            }
            else {
                map.compute(component, (key, current) -> upsertPath(current,
                        components, from + 1, value, created));
            }
            return map;
        }
        else if(index != null && created.contains(ours)) {
            // No other code can reach this list, so changing it in place has
            // the same visible result as the new ArrayList that
            // MergeStrategies#upsert would make with the same elements.
            List<Object> list = (List<Object>) ours;
            // #wrap puts the value for a negative index at position 0.
            int position = Math.max(index, 0);
            if(position < list.size()) {
                Object current = list.get(position);
                list.set(position,
                        isLast ? MergeStrategies.upsert(current, value)
                                : upsertPath(current, components, from + 1,
                                        value, created));
            }
            else {
                // Build the element before padding, so that a supplier that
                // throws leaves the list as it was.
                Object element = isLast ? value
                        : wrap(components, from + 1, value);
                while (list.size() < position) {
                    list.add(null);
                }
                list.add(element);
            }
            return list;
        }
        else {
            Object theirs = wrap(components, from, value);
            Object upserted = MergeStrategies.upsert(ours, theirs);
            if(ours instanceof Collection && isPlainList(theirs)) {
                // MergeStrategies#upsert merged two lists into a new ArrayList,
                // which no other code can reach. The incoming list held each
                // element at its position, so a later write into the new list
                // can update it in place with the same result.
                created.add((Collection<Object>) upserted);
            }
            return upserted;
        }
    }

    /**
     * Return new containers that hold the {@code value} at the end of the
     * {@code components}, from index {@code from} on. An integer component gets
     * a {@link Collection} from {@link #collectionSupplier()} that holds as
     * many {@code null} values as the integer, followed by the nested value.
     * Any other component gets a {@link Map} from {@link #mapSupplier()} that
     * maps the component to the nested value.
     * 
     * @param components the components of a path
     * @param from the index of the outermost component to build a container for
     * @param value the value at the end of the path
     * @return the container for the component at {@code from}
     */
    @SuppressWarnings("unchecked")
    private Object wrap(String[] components, int from, Object value) {
        Object wrapped = value;
        for (int i = components.length - 1; i >= from; --i) {
            String component = components[i];
            Integer index = Ints.tryParse(component);
            Object container = index != null ? collectionSupplier().get()
                    : mapSupplier().get();
            if(container instanceof Collection) {
                Collection<Object> collection = (Collection<Object>) container;
                for (int j = 0; j < index; ++j) {
                    collection.add(null);
                }
                collection.add(wrapped);
            }
            else {
                ((Map<String, Object>) container).put(component, wrapped);
            }
            wrapped = container;
        }
        return wrapped;
    }

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
