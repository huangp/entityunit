package com.github.huangp.entityunit.maker;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A maker that will return interval value from a starting point.
 * This class has only factory methods.
 *
 * @author Patrick Huang
 */
public abstract class IntervalValuesMaker<T> implements Maker<T> {

    /**
     * Example:
     * <pre>
     * {@code
     *
     * public void canGetIntervalInteger() {
     *     Maker<Integer> maker = IntervalValuesMaker.startFrom(1, 2);
     *     assertThat(maker.value(), Matchers.equalTo(1));
     *     assertThat(maker.value(), Matchers.equalTo(3));
     *     assertThat(maker.value(), Matchers.equalTo(5));
     * }
     *
     * public void canGetIntervalLong() {
     *     Maker<Long> maker = IntervalValuesMaker.startFrom(1L, 2);
     *     assertThat(maker.value(), Matchers.equalTo(1L));
     *     assertThat(maker.value(), Matchers.equalTo(3L));
     *     assertThat(maker.value(), Matchers.equalTo(5L));
     * }
     *
     * public void canGetIntervalDate() {
     *     Date start = new Date();
     *     // hint: could use IntervalValuesMaker.startFrom(new Date(), -TimeUnit.DAYS.toMillis(1))
     *     Maker<Date> maker = IntervalValuesMaker.startFrom(start, -1000);
     *     assertThat(maker.value().getTime(), Matchers.equalTo(start.getTime()));
     *     assertThat(maker.value().getTime(), Matchers.equalTo(start.getTime() - 1000));
     *     assertThat(maker.value().getTime(), Matchers.equalTo(start.getTime() - 2000));
     * }
     *
     * public void canGetIntervalString() {
     *     Maker<String> maker = IntervalValuesMaker.startFrom("hello ", 1000);
     *     assertThat(maker.value(), Matchers.equalTo("hello 1000"));
     *     assertThat(maker.value(), Matchers.equalTo("hello 2000"));
     *     assertThat(maker.value(), Matchers.equalTo("hello 3000"));
     * }
     * }
     * </pre>
     *
     * @param start
     *         starting value
     * @param difference
     *         interval difference
     * @param <T>
     *         value type
     * @return a Maker will make interval values indefinitely
     */
    public static <T> Maker<T> startFrom(T start, long difference) {
        if (Integer.class.isInstance(start)) {
            return new IntervalIntegerValuesMaker<T>(start, difference);
        }
        if (Long.class.isInstance(start)) {
            return new IntervalLongValuesMaker<T>(start, difference);
        }
        if (Date.class.isInstance(start)) {
            return new IntervalDateValuesMaker<T>(start, difference);
        }
        if (String.class.isInstance(start)) {
            return new IntervalStringValuesMaker<T>((String) start, difference);
        }
        throw new UnsupportedOperationException("only support Number, Date and String type");
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class IntervalIntegerValuesMaker<T> implements Maker<T> {
        private final T start;
        private final long difference;
        private Integer current;

        @Override
        public T value() {
            next();
            return (T) current;
        }

        private synchronized void next() {
            if (current == null) {
                current = (Integer) start;
            } else {
                current = Long.valueOf(current + difference).intValue();
            }
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class IntervalLongValuesMaker<T> implements Maker<T> {
        private final T start;
        private final long difference;
        private Long current;

        @Override
        public T value() {
            next();
            return (T) current;
        }

        private synchronized void next() {
            if (current == null) {
                current = (Long) start;
            } else {
                current = current + difference;
            }
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class IntervalDateValuesMaker<T> implements Maker<T> {
        private final T start;
        private final long difference;
        private Date current;

        @Override
        public T value() {
            next();
            return (T) current;
        }

        private synchronized void next() {
            if (current == null) {
                current = (Date) start;
            } else {
                current = new Date(current.getTime() + difference);
            }
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class IntervalStringValuesMaker<T> implements Maker<T> {
        private final String start;
        private final long difference;
        private AtomicLong current = new AtomicLong(0);

        @Override
        public T value() {
            return (T) (start + current.addAndGet(difference));
        }

    }
}
