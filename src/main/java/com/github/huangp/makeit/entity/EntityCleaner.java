package com.github.huangp.makeit.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.Query;

import com.github.huangp.makeit.util.ClassUtil;
import com.github.huangp.makeit.util.HasAnnotationPredicate;
import com.github.huangp.makeit.util.Settable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public final class EntityCleaner
{
   private EntityCleaner()
   {
   }

   public static void deleteAll(final EntityManager entityManager, Iterable<Class> entityClasses)
   {
      entityManager.getTransaction().begin();
      for (Class entityType : entityClasses)
      {
         EntityClass entityClass = EntityClass.from(entityType);

         // TODO combine these two
         // delete many to many tables
         Iterable<String> manyToManyTables = entityClass.getManyToManyTables();
         for (String table : manyToManyTables)
         {
            deleteTable(entityManager, table);
         }
         
         // delete element collection
         Iterable<Settable> elementCollection = Iterables.filter(entityClass.getElements(), HasAnnotationPredicate.has(ElementCollection.class));
         for (Settable settable : elementCollection)
         {
            String table = settable.getAnnotation(JoinTable.class).name();
            deleteTable(entityManager, table);
         }

         deleteEntity(entityManager, entityType.getSimpleName());
      }

      entityManager.getTransaction().commit();
   }

   public static void deleteAllExcept(EntityManager entityManager, Iterable<Class> entityClasses, Object... excludedEntities)
   {
      if (excludedEntities.length == 0)
      {
         deleteAll(entityManager, entityClasses);
      }
      ImmutableListMultimap<Class, Object> exclusion = Multimaps.index(ImmutableSet.copyOf(excludedEntities), new Function<Object, Class>()
      {
         @Override
         public Class apply(Object input)
         {
            return input.getClass();
         }
      });

      entityManager.getTransaction().begin();
      for (Class entityType : entityClasses)
      {
         EntityClass entityClass = EntityClass.from(entityType);
         Iterable<String> manyToManyTables = entityClass.getManyToManyTables();

         Settable idSettable = Iterables.find(entityClass.getElements(), HasAnnotationPredicate.has(Id.class));
         List<Serializable> ids = getIds(exclusion.get(entityType), idSettable);

         for (String table : manyToManyTables)
         {
            // TODO need to consider exclusion as well
            deleteTable(entityManager, table);
         }
         // TODO element collection
         deleteEntityExcept(entityManager, entityType.getSimpleName(), exclusion.get(entityType), idSettable, ids);
      }

      entityManager.getTransaction().commit();
   }

   private static void deleteTable(EntityManager entityManager, String table)
   {
      String sqlString = "delete from " + table;
      Query nativeQuery = entityManager.createNativeQuery(sqlString);
      int result = nativeQuery.executeUpdate();
      log.debug("execute [{}], affected row: {}", sqlString, result);
   }

   private static void deleteEntity(EntityManager entityManager, String name)
   {
      String queryString = "delete from " + name;
      int result = entityManager.createQuery(queryString).executeUpdate();
      log.debug("execute [{}], affected row: {}", queryString, result);
   }

   private static void deleteEntityExcept(EntityManager entityManager, String name, List<Object> exclusion, Settable idSettable, List<Serializable> ids)
   {
      if (exclusion.isEmpty())
      {
         deleteEntity(entityManager, name);
         return;
      }
      String queryString = String.format("delete %s e where e.%s not in (:excludedIds)", name, idSettable.getSimpleName());
      int result = entityManager.createQuery(queryString).setParameter("excludedIds", ids).executeUpdate();
      log.debug("executed [{}], affected row: {}", queryString, result);
   }

   private static List<Serializable> getIds(List<Object> entities, final Settable idSettable)
   {
      return Lists.transform(entities, new Function<Object, Serializable>()
      {
         @Override
         public Serializable apply(Object input)
         {
            return ClassUtil.invokeGetter(input, idSettable.getterMethod(), Serializable.class);
         }
      });
   }
}
