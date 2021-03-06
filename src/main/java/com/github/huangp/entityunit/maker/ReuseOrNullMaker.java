package com.github.huangp.entityunit.maker;

import com.github.huangp.entityunit.holder.BeanValueHolder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ReuseOrNullMaker implements Maker<Object> {
    private final BeanValueHolder holder;

    private final Class type;

    @Override
    public Object value() {
        return holder.tryGet(type).orNull();
    }
}
