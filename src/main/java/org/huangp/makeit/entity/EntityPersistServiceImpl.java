package org.huangp.makeit.entity;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.persistence.EntityManager;

import org.huangp.makeit.holder.BeanValueHolder;
import org.huangp.makeit.holder.BeanValueHolderImpl;
import org.huangp.makeit.maker.BeanMaker;
import org.huangp.makeit.scanner.ClassScanner;
import org.huangp.makeit.scanner.EntityClass;
import org.huangp.makeit.scanner.EntityClassScanner;
import org.huangp.makeit.util.ClassUtil;
import org.jodah.typetools.TypeResolver;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import com.google.common.reflect.TypeToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class EntityPersistServiceImpl implements EntityPersistService
{
   private final ClassScanner scanner = new EntityClassScanner();

   @Override
   public Queue<Object> getRequiredEntitiesFor(Class askingClass)
   {
      Iterable<EntityClass> dependingEntities = scanner.scan(askingClass);
      Queue<Object> queue = Queues.newLinkedBlockingQueue();

      // create all depending (ManyToOne or required OneToOne) entities
      BeanValueHolder holder = new BeanValueHolderImpl();
      for (EntityClass entityClass : dependingEntities)
      {
         Serializable entity = new BeanMaker<Serializable>(entityClass.getType(), holder).value();
         holder.putIfNotNull(TypeToken.of(entityClass.getType()), entity);
         queue.offer(entity);
      }
      Serializable askingEntity = new BeanMaker<Serializable>(askingClass, holder).value();

      holder.putIfNotNull(askingClass, askingEntity);
      queue.offer(askingEntity);

      // now work backwards to fill in the one to many side
      for (EntityClass entityNode : dependingEntities)
      {
         Object entity = holder.tryGet(entityNode.getType()).get();

         Iterable<Method> getterMethods = entityNode.getContainingEntitiesGetterMethods();
         for (Method method : getterMethods)
         {
            Type returnType = method.getGenericReturnType();
            if (ClassUtil.isCollection(returnType))
            {
               addManySideEntityIfExists(entity, method, holder);
            }
            if (ClassUtil.isMap(returnType))
            {
               putManySideEntityIfExists(entity, method, holder);
            }
         }
      }
      // required OneToOne mapping should have been set on entity creation
      // @see SingleEntityMaker
      // @see ReuseOrNullMaker

      log.debug("result {}", new QueuePrinter(queue));
      return queue;
   }

   @Override
   public void persistInOrder(EntityManager entityManager, Queue<Object> queue)
   {
      entityManager.getTransaction().begin();
      for (Object entity : queue)
      {
         entityManager.persist(entity);
      }
      entityManager.getTransaction().commit();
   }

   @Override
   public <T> T createAndPersist(EntityManager entityManager, Class<T> entityType)
   {
      Queue<Object> entities = getRequiredEntitiesFor(entityType);
      persistInOrder(entityManager, entities);
      return findEntity(entities, entityType).get();
   }

   public static <T> Optional<T> findEntity(Queue<Object> entityQueue, Class<T> typeToFind)
   {
      List<Object> entities = ImmutableList.copyOf(entityQueue);
      return (Optional<T>) Iterables.tryFind(entities, Predicates.instanceOf(typeToFind));
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
