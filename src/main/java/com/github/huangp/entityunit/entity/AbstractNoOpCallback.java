package com.github.huangp.entityunit.entity;

import javax.persistence.EntityManager;

/**
 * Default implementation that does nothing.
 *
 * @author Patrick Huang
 */
public class AbstractNoOpCallback implements EntityMaker.Callback {
    public static final AbstractNoOpCallback NO_OP_CALLBACK = new AbstractNoOpCallback();

    @Override
    public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted) {
        return toBePersisted;
    }

    @Override
    public Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted) {
        return persisted;
    }
}
