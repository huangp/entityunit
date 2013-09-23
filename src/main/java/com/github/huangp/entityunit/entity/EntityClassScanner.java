package com.github.huangp.entityunit.entity;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * @author Patrick Huang
 */
@Slf4j
class EntityClassScanner {
    private static Cache<CacheKey, Iterable<EntityClass>> cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();
    private final ScanOption scanOption;

    public EntityClassScanner(ScanOption scanOption) {
        this.scanOption = scanOption;
    }

    public EntityClassScanner() {
        this(ScanOption.IgnoreOptionalOneToOne);
    }

    public Iterable<EntityClass> scan(final Class clazz) {
        try {
            return cache.get(CacheKey.of(clazz, scanOption), new Callable<Iterable<EntityClass>>() {
                @Override
                public Iterable<EntityClass> call() throws Exception {
                    return doRealScan(clazz);
                }
            });
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    private Iterable<EntityClass> doRealScan(Class clazz) {
        List<EntityClass> current = Lists.newArrayList();
        recursiveScan(clazz, current);
        return ImmutableList.copyOf(current);
    }

    private void recursiveScan(final Class clazz, List<EntityClass> current) {
        String classUnderScan = clazz.getName();
        log.debug("scanning class: {}", classUnderScan);

        Annotation entityAnnotation = clazz.getAnnotation(Entity.class);
        Preconditions.checkState(entityAnnotation != null, "This scans only entity class");

        EntityClass startNode = getOrCreateNode(current, clazz);
        if (current.contains(startNode)) {
            log.trace("{} has been scanned", startNode);
            return;
        }
        Iterable<EntityClass> dependingTypes = startNode.getDependingEntityTypes();

        for (EntityClass dependingType : dependingTypes) {
            if (!dependingType.getType().equals(clazz)) {
                recursiveScan(dependingType.getType(), current);
            }
        }
        //      Iterable<EntityClass> dependents = Iterables.transform(dependingTypes, new Function<Class<?>, EntityClass>()
        //      {
        //         @Override
        //         public EntityClass apply(Class<?> input)
        //         {
        //            return EntityClass.from(input, scanOption);
        //         }
        //      });

        Iterables.addAll(current, dependingTypes);
        current.remove(startNode); // remove itself
    }

    private EntityClass getOrCreateNode(List<EntityClass> current, final Class<?> entityType) {
        Optional<EntityClass> dependingOptional = Iterables.tryFind(current, new Predicate<EntityClass>() {
            @Override
            public boolean apply(EntityClass input) {
                return input.getType() == entityType;
            }
        });
        return dependingOptional.isPresent() ? dependingOptional.get() : EntityClass.from(entityType, scanOption);
    }

}
