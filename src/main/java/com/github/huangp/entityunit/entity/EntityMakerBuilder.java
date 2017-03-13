package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.maker.Maker;
import com.github.huangp.entityunit.maker.PreferredValueMakersRegistry;
import com.google.common.collect.ImmutableList;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Builder for EntityMaker.
 * <p>
 * Once built the EntityMaker is immutable.
 *
 * @author Patrick Huang
 * @see MakeContext
 * @see PreferredValueMakersRegistry
 * @see BeanValueHolder
 */
@NoArgsConstructor(staticName = "builder")
@Slf4j
public class EntityMakerBuilder {
    private ScanOption scanOption = ScanOption.IgnoreOptionalOneToOne;
    private BeanValueHolder valueHolder = new BeanValueHolder();
    private PreferredValueMakersRegistry registry = new PreferredValueMakersRegistry();

    /**
     * This is the default option. When making entities, unless there is reusable entity, optional OneToOne mapped entity will be ignored.
     *
     * @return this
     */
    public EntityMakerBuilder ignoreOptionalOneToOne() {
        scanOption = ScanOption.IgnoreOptionalOneToOne;
        return this;
    }

    /**
     * When making entities, optional OneToOne mapped entity will also be created.
     * TODO OneToOne mapped entity will always bypass reusable entities even if the entity qualifies reusability (i.e. not referencing other entity)
     * @return this
     */
    public EntityMakerBuilder includeOptionalOneToOne() {
        scanOption = ScanOption.IncludeOneToOne;
        return this;
    }

    /**
     * Reuse objects from another BeanValueHolder.
     *
     * @param beanValueHolder
     *         another bean value holder
     * @return this
     */
    public EntityMakerBuilder reuseObjects(BeanValueHolder beanValueHolder) {
        valueHolder.merge(beanValueHolder);
        return this;
    }

    /**
     * Reuse a collection of objects when making new entities.
     *
     * @param entities
     *         reusable collection of entities
     * @return this
     */
    public EntityMakerBuilder reuseEntities(Collection<Object> entities) {
        for (Object entity : entities) {
            Class aClass = entity.getClass();
            valueHolder.putIfNotNull(aClass, entity);
        }
        return this;
    }

    /**
     * Reuse single entity.
     *
     * @param entity
     *         entity
     * @return this
     */
    public EntityMakerBuilder reuseEntity(Serializable entity) {
        Class aClass = entity.getClass();
        valueHolder.putIfNotNull(aClass, entity);
        return this;
    }

    /**
     * Reuse entities.
     *
     * @param first
     *         first
     * @param second
     *         second
     * @param rest
     *         rest of reusable entities as var args
     * @return this
     */
    public EntityMakerBuilder reuseEntities(Object first, Object second, Object... rest) {
        List<Object> objects = ImmutableList.builder().add(first).add(second).add(rest).build();
        return reuseEntities(objects);
    }

    /**
     * @see PreferredValueMakersRegistry#addFieldOrPropertyMaker(Class, String, Maker)
     */
    public EntityMakerBuilder addFieldOrPropertyMaker(Class<?> ownerType, String fieldName, Maker<?> maker) {
        registry.addFieldOrPropertyMaker(ownerType, fieldName, maker);
        return this;
    }

    /**
     * @see PreferredValueMakersRegistry#addConstructorParameterMaker(Class, int, Maker)
     */
    public EntityMakerBuilder addConstructorParameterMaker(Class<?> ownerType, int argIndex, Maker<?> maker) {
        registry.addConstructorParameterMaker(ownerType, argIndex, maker);
        return this;
    }

    /**
     * Merge another registry in.
     *
     * @param other
     *         other preferred value makers registry
     * @return this
     */
    public EntityMakerBuilder reusePreferredValueMakers(PreferredValueMakersRegistry other) {
        registry.merge(other);
        return this;
    }

    /**
     * @return EntityMaker
     */
    public EntityMaker build() {

        log.debug("registry: {}", registry);
        log.debug("bean value holder: {}", valueHolder);
        EntityClassScanner scanner = new EntityClassScanner(scanOption);
        MakeContext context = new MakeContext(valueHolder, registry);
        return new EntityMakerImpl(scanner, context);
    }
}
