package com.github.huangp.entityunit.holder;

import com.github.huangp.entityunit.entity.EntityMakerBuilder;
import com.github.huangp.entityunit.maker.ScalarValueMakerFactory;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

/**
 * Holds created beans so that it can be reused.
 *
 * @see EntityMakerBuilder
 * @see ScalarValueMakerFactory
 *
 * @author Patrick Huang
 */
public class BeanValueHolder {

    private Map<Class<?>, Object> map = Collections.synchronizedMap(Maps.<Class<?>, Object>newIdentityHashMap());

    public <T> BeanValueHolder putIfNotNull(Class<T> type, T bean) {
        if (bean != null) {
            map.put(type, bean);
        }
        return this;
    }

    public <T> Optional<T> tryGet(Class<T> type) {
        T instance = (T) map.get(type);
        return Optional.fromNullable(instance);
    }

    public void clear() {
        map.clear();
    }

    public BeanValueHolder merge(BeanValueHolder other) {
        map.putAll(other.map);
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("map", map)
                .toString();
    }
}
