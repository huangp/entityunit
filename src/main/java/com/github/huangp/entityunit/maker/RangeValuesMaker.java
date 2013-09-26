package com.github.huangp.entityunit.maker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.Iterator;

/**
 * Define a range of values to return.
 *
 * @author Patrick Huang
 */
public class RangeValuesMaker<T> implements Maker<T> {
    private final Iterator<T> iterator;

    private RangeValuesMaker(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public T value() {
        return iterator.next();
    }

    /**
     * Factory method. Given a list of values and the produced maker will cycle them infinitely.
     *
     * @param first
     *         first value
     * @param rest
     *         rest of the values
     * @param <T>
     *         value type
     * @return a RangeValuesMaker
     */
    public static <T> Maker<T> cycle(T first, T... rest) {
        ImmutableList<T> values = ImmutableList.<T>builder().add(first).add(rest).build();
        return new RangeValuesMaker<T>(Iterables.cycle(values).iterator());
    }

    /**
     * Factory method. Given a list of values and the produced maker will exhaust them before throwing an exception.
     *
     * @param first
     *         first value
     * @param rest
     *         rest of the values
     * @param <T>
     *         value type
     * @return a RangeValuesMaker
     */
    public static <T> Maker<T> errorOnEnd(T first, T... rest) {
        ImmutableList<T> values = ImmutableList.<T>builder().add(first).add(rest).build();
        return new RangeValuesMaker<T>(values.iterator());
    }
}
