package com.github.huangp.entityunit.maker;

/**
 * This is a special maker, It will always return a constant.
 * When BeanMaker is making field values and sees this special constant, it will bypass that field.
 * This is a workaround for skipping field value population...
 *
 * @author Patrick Huang
 */
public enum SkipFieldValueMaker implements Maker<Object> {
    MAKER;

    private static final Object SKIP_MARKER = new Object();

    @Override
    public Object value() {
        return SKIP_MARKER;
    }

    /**
     * Check to see if the generated value should be discard and field population should be bypassed.
     *
     * @param value
     *         generated value from a maker
     * @return true if the generated value is from this maker otherwise false
     */
    public static boolean shouldSkipThisField(Object value) {
        return value == SKIP_MARKER;
    }
}
