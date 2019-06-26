package com.cinchapi.common.collect.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class JITFilterMap<Key, Value> implements Map<Key, Value> {
    private Map<Key, Value> map;
    private final Predicate<Value> filter;
    private final AtomicBoolean isOriginal = new AtomicBoolean(true);

    public JITFilterMap(Map<Key, Value> x, Predicate<Value> filter) {
        this.map = x;
        this.filter = filter;
    }

    @Nullable
    @Override
    public Value put(Key key, Value value) {
        maybeCreateNewMap(value);
        return map.put(key, value);
    }

    @Override
    public void putAll(@NotNull Map<? extends Key, ? extends Value> m) {
        m.forEach((k,v) -> {
            maybeCreateNewMap(v);
            map.put(k, v);
        });
    }

    @Override
    public boolean replace(Key key, Value oldValue, Value newValue) {
        maybeCreateNewMap(newValue);
        return map.replace(key, oldValue, newValue);
    }

    @Nullable
    @Override
    public Value replace(Key key, Value value) {
        maybeCreateNewMap(value);
        return map.replace(key, value);
    }

    @Override
    public void replaceAll(BiFunction<? super Key, ? super Value, ? extends Value> f) {
        map.forEach((k, v) -> {
            maybeCreateNewMap(v);
            f.apply(k, v);
        });
    }

    @Nullable
    @Override
    public Value putIfAbsent(Key key, Value value) {
        maybeCreateNewMap(value);
        return map.putIfAbsent(key, value);
    }

    @Override
    public Value computeIfAbsent(Key key, Function<? super Key, ? extends Value> f) {
        return compute(key, v -> v != null, v -> f.apply(key));
    }

    @Override
    public Value computeIfPresent(Key key, BiFunction<? super Key, ? super Value, ? extends Value> f) {
        return compute(key, v -> v == null, v -> f.apply(key, v));
    }

    @Override
    public Value compute(Key key, BiFunction<? super Key, ? super Value, ? extends Value> f) {
        return compute(key, v -> false, v -> f.apply(key, v));
    }

    @Override
    public Value merge(Key key, Value value, BiFunction<? super Value, ? super Value, ? extends Value> f) {
        Value oldValue = map.get(key);
        Value newValue = oldValue == null ? value : f.apply(oldValue, value);

        if (newValue == null)
            map.remove(key);
        else {
            maybeCreateNewMap(newValue);
            map.put(key, newValue);
        }

        return newValue;
    }

    private Value compute(Key key, Predicate<Value> check, Function<Value, Value> getNewValue) {
        final Value oldValue = map.get(key);
        if (check.test(oldValue))
            return oldValue;
        final Value value = getNewValue.apply(oldValue);
        map.put(key, value);
        maybeCreateNewMap(value);
        return value;
    }

    private void maybeCreateNewMap(Value value) {
        if (isOriginal.get() && !filter.test(value)) {
            map = new HashMap<>(map);
            isOriginal.set(false);
        }
    }
    // ~~ Simple method delegation below this line ~~

    @Override
    public int size() { return map.size(); }

    @Override
    public boolean isEmpty() { return map.isEmpty(); }

    @Override
    public boolean containsKey(Object key) { return map.containsKey(key); }

    @Override
    public boolean containsValue(Object value) { return map.containsValue(value); }

    @Override
    public Value get(Object key) { return map.get(key); }

    @Override
    public Value remove(Object key) { return map.remove(key); }

    @Override
    public void clear() { map.clear(); }

    @NotNull
    @Override
    public Set<Key> keySet() { return map.keySet(); }

    @NotNull
    @Override
    public Collection<Value> values() { return map.values(); }

    @NotNull
    @Override
    public Set<Entry<Key, Value>> entrySet() { return map.entrySet(); }

    @Override
    public boolean equals(Object o) { return map.equals(o); }

    @Override
    public int hashCode() { return map.hashCode(); }

    @Override
    public Value getOrDefault(Object key, Value defaultValue) { return map.getOrDefault(key, defaultValue); }

    @Override
    public void forEach(BiConsumer<? super Key, ? super Value> action) { map.forEach(action); }

    @Override
    public boolean remove(Object key, Object value) { return map.remove(key, value); }




}
