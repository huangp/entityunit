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
import com.github.huangp.makeit.util.ClassUtil;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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

      log.debug("result {}", new QueuePrinter(queue));
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
   public void wireManyToMany(EntityManager entityManager, Object one, Object other)
   {
      addManyToMany(one, other);
      addManyToMany(other, one);
      entityManager.getTransaction().begin();
      for (Object entity : Lists.newArrayList(one, other))
      {
         entityManager.persist(entity);
      }
      entityManager.getTransaction().commit();
   }

   private static void addManyToMany(Object manyOwner, final Object manyElement)
   {
      EntityClass oneEntityClass = EntityClass.from(manyOwner.getClass());

      Iterable<Method> manyToManyGetters = oneEntityClass.getManyToManyMethods();

      Optional<Method> methodFound = Iterables.tryFind(manyToManyGetters, new Predicate<Method>()
      {
         @Override
         public boolean apply(Method input)
         {
            Class<?> genericType = TypeResolver.resolveRawArgument(input.getGenericReturnType(), Collection.class);
            return genericType.isInstance(manyElement);
         }
      });
      if (methodFound.isPresent())
      {
         Collection collection = ClassUtil.invokeGetter(manyOwner, methodFound.get(), Collection.class);
         if (collection != null)
         {
            collection.add(manyElement);
         }
      }
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
   public BeanValueHolder exportImmutableCopyOfBeans()
   {
      return valueHolder.immutableCopy();
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
