package com.github.huangp.entityunit.maker;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @author Patrick Huang
 */
class EnumMaker implements Maker<Object> {
    private final List<Object> enums;

    public EnumMaker(Object[] enumConstants) {
        enums = ImmutableList.copyOf(enumConstants);
    }

    @Override
    public Object value() {
        return enums.get(0);
    }
}
