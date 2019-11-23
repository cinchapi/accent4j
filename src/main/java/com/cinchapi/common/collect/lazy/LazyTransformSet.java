/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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
package com.cinchapi.common.collect.lazy;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.cinchapi.common.base.ReadOnlyIterator;
import com.cinchapi.common.base.Verify;
import com.google.common.collect.Lists;

/**
 * A {@link LazyTransformSet} reads through to another {@link Set} and
 * transforms its items on the fly.
 * <p>
 * This {@link Set} allows just-in-time transformation and should be used to
 * transform elements on the fly when said transformation is expensive. This is
 * especially useful in {@link #stream() stream} operations that feature
 * intermediate
 * operations like {@link Stream#skip(long) skipping}.
 * </p>
 *
 * @author Jeff Nelson
 */
public class LazyTransformSet<F, T> extends AbstractSet<T> {

    /**
     * Return a {@link LazyTransformSet} that uses the {@code transformer} to
     * transform the items in {@code from} on the fly.
     * 
     * @param from
     * @param transformer
     * @return the {@link LazyTransformSet}
     */
    public static <F, T> LazyTransformSet<F, T> of(Set<F> from,
            Function<F, T> transformer) {
        return new LazyTransformSet<>(from, transformer);
    }

    /**
     * The original {@link Set} whose items will be transformed.
     */
    private final Set<F> from;

    /**
     * A cache of the values that have already been transformed, indexed by
     * the iteration order.
     */
    private final List<T> cache;

    /**
     * The transforming function.
     */
    private final Function<F, T> transformer;

    /**
     * Construct a new instance.
     * 
     * @param from
     * @param transformer
     */
    private LazyTransformSet(Set<F> from, Function<F, T> transformer) {
        this.from = from;
        this.transformer = transformer;
        this.cache = Lists.newArrayList();
    }

    @Override
    public Iterator<T> iterator() {
        return new SkippableTransformIterator<>(from.iterator(), transformer,
                cache);
    }

    @Override
    public int size() {
        return from.size();
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), size(), 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<T> stream() {
        Iterator<T> source = iterator();
        Spliterator<T> spliterator = Spliterators.spliterator(source, size(),
                0);
        Stream<T> stream = StreamSupport.stream(spliterator, false);
        return new SkipTrackingStream<>(stream,
                (SkippableTransformIterator<F, T>) source);
    }

    /**
     * A {@link TransformIterator} that skips transformations for an ad-hoc
     * number of elements at the beginning.
     *
     * @author Jeff Nelson
     */
    private static class SkippableTransformIterator<F, T>
            extends TransformIterator<F, T> {

        /**
         * The number of elements to skip during the iteration.
         */
        private long skip = 0;

        /**
         * The current iteration index.
         */
        private long index = 0;

        /**
         * A cache of the values that have already been transformed, indexed by
         * the iteration order.
         */
        private final List<T> cache;

        /**
         * Construct a new instance.
         * 
         * @param skip
         * @param from
         * @param transformer
         */
        SkippableTransformIterator(Iterator<F> from, Function<F, T> transformer,
                List<T> cache) {
            super(from, transformer);
            this.cache = cache;
        }

        @Override
        public T next() {
            F next = from.next();
            T transformed;
            if(index >= skip) {
                if(cache.size() < (index + 1)
                        || (transformed = cache.get((int) index)) == null) {
                    transformed = transformer.apply(next);
                    while (cache.size() < index + 1) {
                        cache.add(null);
                    }
                    cache.set((int) index, transformed);
                }
            }
            else {
                // Since the elements are being skipped, the assumption is that
                // they won't be consume, so we don't care about the potential
                // for an NPE.
                transformed = null;
            }
            ++index;
            return transformed;
        }

        /**
         * Note that the next {@code n} elements should be skipped and not
         * transformed
         * 
         * @param n
         * @return this; for chaining
         */
        public final SkippableTransformIterator<F, T> skip(long n) {
            Verify.that(n > -1);
            skip += n;
            return this;
        }

    }

    /**
     * A {@link Stream} that keeps track of the total number of elements that
     * must be {@link #skip(long) skipped} in the underlying source
     * {@link #iterator()}.
     *
     *
     * @author Jeff Nelson
     */
    private static class SkipTrackingStream<F, T> implements Stream<T> {

        /**
         * The {@link Stream} whose skips are being tracked.
         */
        private final Stream<T> stream;

        /**
         * The source {@link Iterator} that the {@link #stream} ultimately reads
         * through to.
         */
        private final SkippableTransformIterator<F, T> source;

        /**
         * Construct a new instance.
         * 
         * @param stream
         * @param source
         */
        SkipTrackingStream(Stream<T> stream,
                SkippableTransformIterator<F, T> source) {
            this.stream = stream;
            this.source = source;
        }

        @Override
        public boolean allMatch(Predicate<? super T> predicate) {
            return stream.allMatch(predicate);
        }

        @Override
        public boolean anyMatch(Predicate<? super T> predicate) {
            return stream.anyMatch(predicate);
        }

        @Override
        public void close() {
            stream.close();
        }

        @Override
        public <R, A> R collect(Collector<? super T, A, R> collector) {
            return stream.collect(collector);
        }

        @Override
        public <R> R collect(Supplier<R> supplier,
                BiConsumer<R, ? super T> accumulator,
                BiConsumer<R, R> combiner) {
            return stream.collect(supplier, accumulator, combiner);
        }

        @Override
        public long count() {
            return stream.count();
        }

        @Override
        public Stream<T> distinct() {
            return stream.distinct();
        }

        @Override
        public Stream<T> filter(Predicate<? super T> predicate) {
            return stream.filter(predicate);
        }

        @Override
        public Optional<T> findAny() {
            return stream.findAny();
        }

        @Override
        public Optional<T> findFirst() {
            return stream.findFirst();
        }

        @Override
        public <R> Stream<R> flatMap(
                Function<? super T, ? extends Stream<? extends R>> mapper) {
            return stream.flatMap(mapper);
        }

        @Override
        public DoubleStream flatMapToDouble(
                Function<? super T, ? extends DoubleStream> mapper) {
            return stream.flatMapToDouble(mapper);
        }

        @Override
        public IntStream flatMapToInt(
                Function<? super T, ? extends IntStream> mapper) {
            return stream.flatMapToInt(mapper);
        }

        @Override
        public LongStream flatMapToLong(
                Function<? super T, ? extends LongStream> mapper) {
            return stream.flatMapToLong(mapper);
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            stream.forEach(action);
        }

        @Override
        public void forEachOrdered(Consumer<? super T> action) {
            stream.forEachOrdered(action);
        }

        @Override
        public boolean isParallel() {
            return stream.isParallel();
        }

        @Override
        public Iterator<T> iterator() {
            return stream.iterator();
        }

        @Override
        public Stream<T> limit(long maxSize) {
            return stream.limit(maxSize);
        }

        @Override
        public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
            return stream.map(mapper);
        }

        @Override
        public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
            return stream.mapToDouble(mapper);
        }

        @Override
        public IntStream mapToInt(ToIntFunction<? super T> mapper) {
            return stream.mapToInt(mapper);
        }

        @Override
        public LongStream mapToLong(ToLongFunction<? super T> mapper) {
            return stream.mapToLong(mapper);
        }

        @Override
        public Optional<T> max(Comparator<? super T> comparator) {
            return stream.max(comparator);
        }

        @Override
        public Optional<T> min(Comparator<? super T> comparator) {
            return stream.min(comparator);
        }

        @Override
        public boolean noneMatch(Predicate<? super T> predicate) {
            return stream.noneMatch(predicate);
        }

        @Override
        public Stream<T> onClose(Runnable closeHandler) {
            return stream.onClose(closeHandler);
        }

        @Override
        public Stream<T> parallel() {
            return stream.parallel();
        }

        @Override
        public Stream<T> peek(Consumer<? super T> action) {
            return stream.peek(action);
        }

        @Override
        public Optional<T> reduce(BinaryOperator<T> accumulator) {
            return stream.reduce(accumulator);
        }

        @Override
        public T reduce(T identity, BinaryOperator<T> accumulator) {
            return stream.reduce(identity, accumulator);
        }

        @Override
        public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator,
                BinaryOperator<U> combiner) {
            return stream.reduce(identity, accumulator, combiner);
        }

        @Override
        public Stream<T> sequential() {
            return stream.sequential();
        }

        @Override
        public Stream<T> skip(long n) {
            return new SkipTrackingStream<>(stream.skip(n), source.skip(n));
        }

        @Override
        public Stream<T> sorted() {
            return stream.sorted();
        }

        @Override
        public Stream<T> sorted(Comparator<? super T> comparator) {
            return stream.sorted(comparator);
        }

        @Override
        public Spliterator<T> spliterator() {
            return stream.spliterator();
        }

        @Override
        public Object[] toArray() {
            return stream.toArray();
        }

        @Override
        public <A> A[] toArray(IntFunction<A[]> generator) {
            return stream.toArray(generator);
        }

        @Override
        public Stream<T> unordered() {
            return stream.unordered();
        }

    }

    /**
     * A {@link ReadOnlyIterator} that reads through and transforms elements
     * from another {@link Iterator} on-the-fly.
     *
     * @author Jeff Nelson
     */
    private static class TransformIterator<F, T> extends ReadOnlyIterator<T> {

        /**
         * An {@link Iterator} over the source elements.
         */
        protected final Iterator<F> from;

        /**
         * The transforming function
         */
        protected final Function<F, T> transformer;

        /**
         * Construct a new instance.
         * 
         * @param from
         * @param transformer
         */
        TransformIterator(Iterator<F> from, Function<F, T> transformer) {
            this.from = from;
            this.transformer = transformer;
        }

        @Override
        public final boolean hasNext() {
            return from.hasNext();
        }

        @Override
        public T next() {
            F next = from.next();
            return transformer.apply(next);
        }

    }

}
