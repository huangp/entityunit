package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.holder.BeanValueHolder;

import javax.persistence.EntityManager;

/**
 * Maker for making and persisting entities.
 *
 * @author Patrick Huang
 * @see EntityMakerBuilder
 */
public interface EntityMaker {
    /**
     * Make the given type of entity and all entities that it depends(references) and then persist.
     * It also make sure associations are populated accordingly.
     * <p/>
     * If called multiple times, the asking entity will be made each time but depending entities will be reused.
     * <pre>
     * {@code
     *
     * // Given:
     * // Category <--- oneToMany --- LineItem
     *
     * EntityMaker maker = EntityMakerBuilder.builder().build();
     * LineItem itemOne = maker.makeAndPersist(entityManager, LineItem.class);
     * LineItem itemTwo = maker.makeAndPersist(entityManager, LineItem.class);
     *
     * // you should have itemOne and itemTwo in database both referencing the same Category record.
     * assertThat(itemOne.getId(), Matchers.notNullValue());
     * assertThat(itemTwo.getId(), Matchers.notNullValue());
     *
     * Category category = itemOne.getCategory();
     * assertThat(category, Matchers.notNullValue());
     * assertThat(category.getId(), Matchers.notNullValue());
     * assertThat(itemOne.getCategory(), Matchers.sameInstance(itemTwo.getCategory()));
     * assertThat(category.getItems(), Matchers.contains(itemOne, itemTwo));
     * }
     * </pre>
     *
     * @param entityManager
     *         entity manager that know about the entity
     * @param entityType
     *         entity type
     * @param <T>
     *         entity type
     * @return made and persisted entity
     */
    <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType);

    /**
     * Similar to {@link EntityMaker#makeAndPersist(EntityManager, Class)} with callback.
     *
     * @param entityManager
     *         entity manager that know about the entity
     * @param entityType
     *         entity type
     * @param callback
     *         callback that can be injected before and after persist
     * @param <T>
     *         entity type
     * @return made and persisted entity
     * @see Callback
     */
    <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback);

    // TODO do we need this anymore? TakeCopyCallback should handle it
    BeanValueHolder exportCopyOfBeans();

    /**
     * Provide callback functionality before and after persistence.
     *
     * @see TakeCopyCallback
     * @see WireManyToManyCallback
     * @see FixIdCallback
     * @see Callbacks
     * @see AbstractNoOpCallback
     */
    interface Callback {
        /**
         * Will be called before persisting made entities.
         * i.e. rewire some associations and fix some things that are not right.
         *
         * @param entityManager
         *         entity manager
         * @param toBePersisted
         *         an iterable of objects made (may reuse some already persisted entities)
         * @return an presumeably altered iterable of objects
         */
        Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted);

        /**
         * Will be called after made entities are persisted.
         *
         * @param entityManager
         *         entity manager
         * @param persisted
         *         an iterable of just persisted objects
         * @return final objects made
         * @see FixIdCallback
         */
        Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted);
    }
}
