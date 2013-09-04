package com.github.huangp.makeit.entity;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;

import org.jodah.typetools.TypeResolver;
import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.BeanMaker;
import com.github.huangp.makeit.util.ClassUtil;
import com.github.huangp.makeit.util.Settable;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.reflect.TypeToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
class EntityPersistServiceImpl implements EntityPersistService
{
   private final EntityClassScanner scanner;
   private final MakeContext context;
   private final BeanValueHolder valueHolder;

   EntityPersistServiceImpl(EntityClassScanner scanner, MakeContext context)
   {
      this.scanner = scanner;
      this.context = context;
      valueHolder = context.getBeanValueHolder();
   }

   @Override
   public <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType)
   {
      Iterable<Object> entities = getRequiredEntitiesFor(entityType);
      persistInOrder(entityManager, entities);
      return ClassUtil.findEntity(entities, entityType);
   }

   private Iterable<Object> getRequiredEntitiesFor(Class askingClass)
   {
      Iterable<EntityClass> dependingEntities = scanner.scan(askingClass);
      Queue<Object> queue = Queues.newLinkedBlockingQueue();

      // create all depending (ManyToOne or required OneToOne) entities
      for (EntityClass entityClass : dependingEntities)
      {
         reuseOrMakeNew(queue, entityClass);
      }
      // we always make new asking class
      Serializable askingEntity = new BeanMaker<Serializable>(askingClass, context).value();

      context.getBeanValueHolder().putIfNotNull(askingClass, askingEntity);
      queue.offer(askingEntity);

      // now work backwards to fill in the one to many side
      for (EntityClass entityNode : dependingEntities)
      {
         Object entity = valueHolder.tryGet(entityNode.getType()).get();

         Iterable<Method> getterMethods = entityNode.getContainingEntitiesGetterMethods();
         for (Method method : getterMethods)
         {
            Type returnType = method.getGenericReturnType();
            if (ClassUtil.isCollection(returnType))
            {
               addManySideEntityIfExists(entity, method, valueHolder);
            }
            if (ClassUtil.isMap(returnType))
            {
               putManySideEntityIfExists(entity, method, valueHolder);
            }
         }
      }
      // required OneToOne mapping should have been set on entity creation
      // @see SingleEntityMaker
      // @see ReuseOrNullMaker

      log.debug("result {}", new NiceIterablePrinter(queue));
      return queue;
   }

   private void reuseOrMakeNew(Queue<Object> queue, EntityClass entityClass)
   {
      Optional existing = valueHolder.tryGet(entityClass.getType());
      if (existing.isPresent())
      {
         queue.offer(existing.get());
      }
      else
      {
         Serializable entity = new BeanMaker<Serializable>(entityClass.getType(), context).value();
         context.getBeanValueHolder().putIfNotNull(TypeToken.of(entityClass.getType()), entity);
         queue.offer(entity);
      }
   }

   private static void persistInOrder(EntityManager entityManager, Iterable<Object> queue)
   {
      entityManager.getTransaction().begin();
      for (Object entity : queue)
      {
         entityManager.persist(entity);
      }
      entityManager.getTransaction().commit();
   }

   @Override
   public <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback)
   {
      Iterable<Object> allObjects = getRequiredEntitiesFor(entityType);
      Iterable<Object> toPersist = callback.beforePersist(entityManager, allObjects);
      persistInOrder(entityManager, toPersist);
      return ClassUtil.findEntity(toPersist, entityType);
   }

   @Override
   public void deleteAll(final EntityManager entityManager, Iterable<Class> entityClasses)
   {
      entityManager.getTransaction().begin();
      for (Class entityType : entityClasses)
      {
         EntityClass entityClass = EntityClass.from(entityType);
         Iterable<String> manyToManyTables = entityClass.getManyToManyTables();
         for (String table : manyToManyTables)
         {
            deleteTable(entityManager, table);
         }
         deleteEntity(entityManager, entityType.getSimpleName());
      }

      entityManager.getTransaction().commit();
   }

   @Override
   public void deleteAllExcept(EntityManager entityManager, Iterable<Class> entityClasses, Object... excludedEntities)
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

         Settable idSettable = Iterables.find(entityClass.getElements(), EntityClass.HasAnnotationPredicate.has(Id.class));
         List<Serializable> ids = getIds(exclusion.get(entityType), idSettable);

         for (String table : manyToManyTables)
         {
            // TODO need to consider exclusion as well
            deleteTable(entityManager, table);
         }
         deleteEntityExcept(entityManager, entityType.getSimpleName(), exclusion.get(entityType), idSettable, ids);
      }

      entityManager.getTransaction().commit();
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

   @Override
   public BeanValueHolder exportCopyOfBeans()
   {
      return valueHolder.getCopy();
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

   private static void addManySideEntityIfExists(Object entity, Method method, BeanValueHolder holder)
   {
      Class<?> genericType = TypeResolver.resolveRawArgument(method.getGenericReturnType(), Collection.class);
      Optional<?> manySideExists = holder.tryGet(genericType);
      if (manySideExists.isPresent())
      {
         Object existValue = manySideExists.get();
         Collection collection = ClassUtil.invokeGetter(entity, method, Collection.class);
         if (collection != null)
         {
            collection.add(existValue);
         }
      }
   }

   private static void putManySideEntityIfExists(Object entity, Method method, BeanValueHolder holder)
   {
      Class<?>[] genericTypes = TypeResolver.resolveRawArguments(method.getGenericReturnType(), Collection.class);
      Class<?> keyType = genericTypes[0];
      Class<?> valueType = genericTypes[1];
      Optional keyOptional = holder.tryGet(keyType);
      Optional valueOptional = holder.tryGet(valueType);
      warningIfTrue(!keyOptional.isPresent(), "You have to manually resolve this: {} {}.{}()", entity, method);

      if (keyOptional.isPresent() && valueOptional.isPresent())
      {
         Object key = keyOptional.get();
         Object value = valueOptional.get();
         Map map = ClassUtil.invokeGetter(entity, method, Map.class);
         if (map != null)
         {
            map.put(key, value);
         }
      }
   }

   private static void warningIfTrue(boolean expression, String logTemplate, Object entity, Method method)
   {
      if (expression)
      {
         log.warn(logTemplate, method.getGenericReturnType(), entity.getClass().getSimpleName(), method.getName());
      }
   }

   @RequiredArgsConstructor
   private static class NiceIterablePrinter
   {
      private static final String NEW_LINE = "\n";
      private static final String THEN = "    ==> ";
      private final Iterable<Object> iterable;

      @Override
      public String toString()
      {
         List<Object> entities = ImmutableList.copyOf(iterable);
         StringBuilder builder = new StringBuilder();
         builder.append(NEW_LINE);
         for (Object entity : entities)
         {
            builder.append(THEN).append(entity);
            builder.append(NEW_LINE);
         }
         return builder.toString();
      }
   }

}
