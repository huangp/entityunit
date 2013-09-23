package com.github.huangp.entityunit.entity;

import com.google.common.collect.ImmutableList;

import javax.persistence.EntityManager;

import static com.github.huangp.entityunit.entity.EntityMaker.Callback;

/**
 * @author Patrick Huang
 */
public final class Callbacks {

    private Callbacks() {
    }

    public static TakeCopyCallback takeCopy() {
        return new TakeCopyCallback();
    }

    public static WireManyToManyCallback wireManyToMany(Class typeToFind, Object objectToWire) {
        return new WireManyToManyCallback(typeToFind, objectToWire);
    }

    public static Callback chain(Callback one, Callback... others) {
        return new ChainedCallback(one, others);
    }

    private static class ChainedCallback implements Callback {
        private final ImmutableList<EntityMaker.Callback> callbacks;

        public ChainedCallback(Callback one, Callback... rest) {
            callbacks = ImmutableList.<Callback>builder().add(one).add(rest).build();
        }

        @Override
        public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted) {
            Iterable<Object> toReturn = toBePersisted;
            for (Callback callback : callbacks) {
                toReturn = callback.beforePersist(entityManager, toBePersisted);
            }
            return toReturn;
        }

        @Override
        public Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted) {
            Iterable<Object> toReturn = persisted;
            for (Callback callback : callbacks) {
                toReturn = callback.afterPersist(entityManager, persisted);
            }
            return toReturn;
        }
    }
}
