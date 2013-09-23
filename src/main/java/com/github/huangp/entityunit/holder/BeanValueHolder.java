package com.github.huangp.entityunit.holder;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.reflect.ImmutableTypeToInstanceMap;
import com.google.common.reflect.MutableTypeToInstanceMap;
import com.google.common.reflect.TypeToInstanceMap;
import com.google.common.reflect.TypeToken;

import java.util.Map;

/**
 * @author Patrick Huang
 */
public class BeanValueHolder {

    private TypeToInstanceMap<Object> map = new MutableTypeToInstanceMap<Object>();

    public <T> BeanValueHolder putIfNotNull(TypeToken<T> typeToken, T bean) {
        if (bean != null) {
            map.putInstance(typeToken, bean);
        }
        return this;
    }

    public <T> Optional<T> tryGet(TypeToken<T> typeToken) {
        T instance = map.getInstance(typeToken);
        return Optional.fromNullable(instance);
    }

    public <T> BeanValueHolder putIfNotNull(Class<T> type, T bean) {
        return putIfNotNull(TypeToken.of(type), bean);
    }

    public <T> Optional<T> tryGet(Class<T> type) {
        return tryGet(TypeToken.of(type));
    }

    public void clear() {
        map.clear();
    }

    public BeanValueHolder merge(BeanValueHolder other) {
        for (Map.Entry<TypeToken<?>, Object> entry : other.map.entrySet()) {
            TypeToken key = entry.getKey();
            this.map.putInstance(key, entry.getValue());
        }
        return this;
    }

    public BeanValueHolder getCopy() {
        ImmutableTypeToInstanceMap.Builder<Object> builder = ImmutableTypeToInstanceMap.builder();
        for (Map.Entry<TypeToken<?>, Object> entry : map.entrySet()) {
            TypeToken key = entry.getKey();
            builder.put(key, entry.getValue());
        }
        BeanValueHolder result = new BeanValueHolder();
        result.map = builder.build();
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("map", map)
                .toString();
    }
}
