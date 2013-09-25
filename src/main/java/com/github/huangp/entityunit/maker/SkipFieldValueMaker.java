package com.github.huangp.entityunit.maker;

/**
 * @author Patrick Huang
 */
public enum SkipFieldValueMaker implements Maker<Object> {
    MAKER;

    private static final Object SKIP_MARKER = new Object();

    @Override
    public Object value() {
        return SKIP_MARKER;
    }

    public static boolean shouldSkipThisField(Object value) {
        return value == SKIP_MARKER;
    }
}
