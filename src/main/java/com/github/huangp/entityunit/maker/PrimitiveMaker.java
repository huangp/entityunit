package com.github.huangp.entityunit.maker;

import com.google.common.base.Defaults;
import com.google.common.base.Preconditions;

import java.lang.reflect.Type;

/**
 * @author Patrick Huang
 */
class PrimitiveMaker implements Maker {
    private final Class<?> type;

    public PrimitiveMaker(Type type) {
        Preconditions.checkArgument(type instanceof Class<?>);
        this.type = (Class<?>) type;
    }

    @Override
    public Object value() {
        return Defaults.defaultValue(type);
    }
}
