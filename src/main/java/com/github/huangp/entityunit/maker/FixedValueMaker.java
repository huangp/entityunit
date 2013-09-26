package com.github.huangp.entityunit.maker;

import com.google.common.base.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Always return fixed value.
 *
 * @author Patrick Huang
 * @see FixedValueMaker#ALWAYS_TRUE_MAKER
 * @see FixedValueMaker#EMPTY_STRING_MAKER
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FixedValueMaker<V> implements Maker<V> {
    public static final FixedValueMaker<Boolean> ALWAYS_TRUE_MAKER = new FixedValueMaker<Boolean>(true);
    public static final FixedValueMaker<String> EMPTY_STRING_MAKER = new FixedValueMaker<String>("");

    private final V fixedValue;

    @Override
    public V value() {
        return fixedValue;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(fixedValue)
                .toString();
    }

    /**
     * Factory method.
     *
     * @param value
     *         the fixed value to make
     * @param <T>
     *         value type
     * @return a fixed value maker
     */
    public static <T> Maker<T> fix(T value) {
        return new FixedValueMaker<T>(value);
    }
}
