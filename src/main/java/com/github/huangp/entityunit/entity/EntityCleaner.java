package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.HasAnnotationPredicate;
import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.List;

import static com.github.huangp.entityunit.util.HasAnnotationPredicate.has;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * Helper class to clear database records.
 *
 * @author Patrick Huang
 */
@Slf4j
public final class EntityCleaner {
    private EntityCleaner() {
    }


    static void deleteAll(EntityManager entityManager, Class... entityClasses) {
        for (Class clazz : entityClasses) {
            EntityClass entityClass = EntityClass.from(clazz, ScanOption.IncludeOneToOne);

        }
        //TODO giving entity classes in arbitrary order and this can sort it out
        throw new UnsupportedOperationException("Implement me!");
    }

    /**
     * Delete all records from given entity representing tables and their many to many and element collection tables.
     * <p/>
     * The entity classes must be in correct order. Item references Category then Category must in front of the iterable.
     *
     * @param entityManager
     *         entity manager
     * @param entityClasses
     *         entity class in correct order
     */
    public static void deleteAll(final EntityManager entityManager, Iterable<Class> entityClasses) {
        entityManager.getTransaction().begin();
        for (Class entityType : entityClasses) {
            EntityClass entityClass = EntityClass.from(entityType);

            // delete many to many and element collection tables
            Iterable<String> associationTables = getAssociationTables(entityClass);
            for (String table : associationTables) {
                deleteTable(entityManager, table);
            }

            deleteEntity(entityManager, ClassUtil.getEntityName(entityType));
        }

        entityManager.getTransaction().commit();
    }

    /**
     * Delete all records from given entity representing tables except exclusion. Exclusion are given as entity object.
     * So a match on id will be used.
     * <p/>
     * TODO manyToMany and element collection table do not consider exlusion yet
     *
     * @param entityManager
     *         entity manager
     * @param entityClasses
     *         entity cless in correct order
     * @param excludedEntities
     *         excluded persisted entity objects
     */
    public static void deleteAllExcept(EntityManager entityManager, Iterable<Class> entityClasses, Object... excludedEntities) {
        if (excludedEntities.length == 0) {
            deleteAll(entityManager, entityClasses);
        }
        ImmutableListMultimap<Class, Object> exclusion = Multimaps.index(ImmutableSet.copyOf(excludedEntities), new Function<Object, Class>() {
            @Override
            public Class apply(Object input) {
                return input.getClass();
            }
        });

        entityManager.getTransaction().begin();
        for (Class entityType : entityClasses) {
            EntityClass entityClass = EntityClass.from(entityType);
            Iterable<String> manyToManyTables = getAssociationTables(entityClass);

            Settable idSettable = Iterables.find(entityClass.getElements(), HasAnnotationPredicate.has(Id.class));
            List<Serializable> ids = getIds(exclusion.get(entityType), idSettable);

            for (String table : manyToManyTables) {
                // TODO need to consider exclusion as well
                deleteTable(entityManager, table);
            }
            // TODO element collection
            deleteEntityExcept(entityManager, entityType.getSimpleName(), exclusion.get(entityType), idSettable, ids);
        }

        entityManager.getTransaction().commit();
    }

    private static void deleteTable(EntityManager entityManager, String table) {
        String sqlString = "delete from " + table;
        Query nativeQuery = entityManager.createNativeQuery(sqlString);
        int result = nativeQuery.executeUpdate();
        log.debug("execute [{}], affected row: {}", sqlString, result);
    }

    private static void deleteEntity(EntityManager entityManager, String name) {
        String queryString = "delete from " + name;
        int result = entityManager.createQuery(queryString).executeUpdate();
        log.debug("execute [{}], affected row: {}", queryString, result);
    }

    private static void deleteEntityExcept(EntityManager entityManager, String name, List<Object> exclusion, Settable idSettable, List<Serializable> ids) {
        if (exclusion.isEmpty()) {
            deleteEntity(entityManager, name);
            return;
        }
        String queryString = String.format("delete %s e where e.%s not in (:excludedIds)", name, idSettable.getSimpleName());
        int result = entityManager.createQuery(queryString).setParameter("excludedIds", ids).executeUpdate();
        log.debug("executed [{}], affected row: {}", queryString, result);
    }

    /**
     * This will find all ManyToMany and ElementCollection annotated tables.
     */
    private static Iterable<String> getAssociationTables(EntityClass entityClass) {
        Iterable<Settable> association = filter(entityClass.getElements(),
                and(or(has(ManyToMany.class), has(ElementCollection.class)), has(JoinTable.class)));
        return transform(association, new Function<Settable, String>() {
            @Override
            public String apply(Settable input) {
                JoinTable annotation = input.getAnnotation(JoinTable.class);
                return annotation.name();
            }
        });
    }

    private static List<Serializable> getIds(List<Object> entities, final Settable idSettable) {
        return Lists.transform(entities, new Function<Object, Serializable>() {
            @Override
            public Serializable apply(Object input) {
                return idSettable.valueIn(input);
            }
        });
    }
}
