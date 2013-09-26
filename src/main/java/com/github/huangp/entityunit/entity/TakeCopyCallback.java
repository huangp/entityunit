package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.ClassUtil;
import com.google.common.collect.ImmutableList;
import lombok.Getter;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Take a copy of the objects and expose it (so that some of the objects can be reused).
 *
 * @author Patrick Huang
 */
public class TakeCopyCallback extends AbstractNoOpCallback {
    @Getter
    private List<Object> copy;

    @Override
    public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted) {
        copy = ImmutableList.copyOf(toBePersisted);
        return toBePersisted;
    }

    /**
     * @param index
     *         index
     * @param <T>
     *         return type
     * @return object at that index in the copy
     */
    public <T> T getByIndex(int index) {
        return (T) copy.get(index);
    }

    /**
     * @param type
     *         wanted class type
     * @param <T>
     *         return type
     * @return object that matches the type in the copy
     */
    public <T> T getByType(Class<T> type) {
        return ClassUtil.findEntity(copy, type);
    }

}
