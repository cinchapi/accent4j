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
    private final AtomicBoolean isOriginal = new AtomicBoolean(true);
    private Map<Key, Value> map;
    final private Predicate<Value> check;

    public JITFilterMap(Map<Key, Value> x, Predicate<Value> check) {
        this.map = x;
        this.check = check;
    }

    @Nullable @Override public Value put(Key key, Value value) {
        executeAndMaybeCreateNewMap(value, () -> map.put(key, value));
        final Value lastValue = map.get(key);
        if (isOriginal.get() && !check.test(value))
            map = new HashMap<>(map);
        map.put(key, value);
        return lastValue;
    }

    @Override public void putAll(@NotNull Map<? extends Key, ? extends Value> m) {
        m.forEach((k,v) -> {
            if (isOriginal.get() && !(check.test(v))) {
                map = new HashMap<>(map);
                isOriginal.set(false);
            }
            map.put(k, v);
        });
    }

    @Override
    public boolean replace(Key key, Value oldValue, Value newValue) {
        AtomicBoolean
        return map.replace(key, oldValue, newValue);
    }

    @Nullable
    @Override
    public Value replace(Key key, Value value) { return map.replace(key, value); }

    @Override
    public void replaceAll(BiFunction<? super Key, ? super Value, ? extends Value> function) {
        map.replaceAll(function);
    }

    @Nullable
    @Override
    public Value putIfAbsent(Key key, Value value) { return map.putIfAbsent(key, value); }

    private <T> T executeAndMaybeCreateNewMap(Value value, Supplier<T> alwaysRun) {
        if (isOriginal.get() && !check.test(value)) {
            map = new HashMap<>(map);
            isOriginal.set(false);
        }
        return alwaysRun.get();
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



    @Override
    public Value computeIfAbsent(Key key, Function<? super Key, ? extends Value> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Value computeIfPresent(Key key, BiFunction<? super Key, ? super Value, ? extends Value> remappingFunction) {
        return map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Value compute(Key key, BiFunction<? super Key, ? super Value, ? extends Value> remappingFunction) {
        return map.compute(key, remappingFunction);
    }

    @Override
    public Value merge(Key key, Value value,
                       BiFunction<? super Value, ? super Value, ? extends Value> remappingFunction) {
        return map.merge(key, value, remappingFunction);
    }


}
