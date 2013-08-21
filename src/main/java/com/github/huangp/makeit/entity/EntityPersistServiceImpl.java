package com.github.huangp.makeit.entity;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jodah.typetools.TypeResolver;
import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.BeanMaker;
import com.github.huangp.makeit.maker.PreferredValueMakersRegistry;
import com.github.huangp.makeit.util.ClassUtil;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class EntityPersistServiceImpl implements EntityPersistService
{
   private final EntityClassScanner scanner;
   private final MakeContext context;

   @Override
   public <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType)
   {
      Queue<Object> entities = getRequiredEntitiesFor(entityType);
      persistInOrder(entityManager, entities);
      return ClassUtil.findEntity(entities, entityType);
   }

   private Queue<Object> getRequiredEntitiesFor(Class askingClass)
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
         Object entity = context.getBeanValueHolder().tryGet(entityNode.getType()).get();

         Iterable<Method> getterMethods = entityNode.getContainingEntitiesGetterMethods();
         for (Method method : getterMethods)
         {
            Type returnType = method.getGenericReturnType();
            if (ClassUtil.isCollection(returnType))
            {
               addManySideEntityIfExists(entity, method, context.getBeanValueHolder());
            }
            if (ClassUtil.isMap(returnType))
            {
               putManySideEntityIfExists(entity, method, context.getBeanValueHolder());
            }
         }
      }
      // required OneToOne mapping should have been set on entity creation
      // @see SingleEntityMaker
      // @see ReuseOrNullMaker

      log.debug("result {}", new QueuePrinter(queue));
      return queue;
   }

   private void reuseOrMakeNew(Queue<Object> queue, EntityClass entityClass)
   {
      Optional existing = context.getBeanValueHolder().tryGet(entityClass.getType());
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

   private static void persistInOrder(EntityManager entityManager, Queue<Object> queue)
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
      Queue<Object> allObjects = getRequiredEntitiesFor(entityType);
      Queue<Object> toPersist = callback.beforePersist(allObjects);
      persistInOrder(entityManager, toPersist);
      return ClassUtil.findEntity(toPersist, entityType);
   }

   @Override
   public void deleteAll(final EntityManager entityManager, Iterable<Class> entities)
   {
      entityManager.getTransaction().begin();
      for (Class entity : entities)
      {
         // TODO need to consider @Entity(name)
         EntityClass entityClass = EntityClass.from(entity);
         Iterable<String> manyToManyTables = entityClass.getManyToManyTables();
         for (String table : manyToManyTables)
         {
            deleteTable(entityManager, table);
         }
         deleteEntity(entityManager, entity.getSimpleName());
      }

      entityManager.getTransaction().commit();
   }

   @Override
   public MakeContext getCurrentContext()
   {
      return context;
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

   private static void addManySideEntityIfExists(Object entity, Method method, BeanValueHolder holder)
   {
      Class<?> genericType = TypeResolver.resolveRawArgument(method.getGenericReturnType(), Collection.class);
      Optional<?> manySideExists = holder.tryGet(genericType);
      if (manySideExists.isPresent())
      {
         Object existValue = manySideExists.get();
         Collection collection = invokeGetter(entity, method, Collection.class);
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
         Map map = invokeGetter(entity, method, Map.class);
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

   private static <T> T invokeGetter(Object entity, Method method, Class<T> getterReturnType)
   {
      try
      {
         T result = (T) method.invoke(entity);
         warningIfTrue(result == null, "after invoke getter {} {}.{}() the field is still null", entity, method);
         return result;
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
   }

   @RequiredArgsConstructor
   private static class QueuePrinter
   {
      private static final String NEW_LINE = "\n";
      private static final String THEN = "    ==> ";
      private final Queue<Object> queue;

      @Override
      public String toString()
      {
         List<Object> entities = ImmutableList.copyOf(queue);
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
