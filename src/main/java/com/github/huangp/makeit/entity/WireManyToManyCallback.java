package com.github.huangp.makeit.entity;

import java.lang.reflect.Method;
import java.util.Collection;
import javax.persistence.EntityManager;

import org.jodah.typetools.TypeResolver;
import com.github.huangp.makeit.util.ClassUtil;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@RequiredArgsConstructor
public class WireManyToManyCallback implements EntityPersister.Callback
{
   private final Class typeToFind;
   private final Object objectToWire;

   @Override
   public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted)
   {
      Object target = ClassUtil.findEntity(toBePersisted, typeToFind);
      addManyToMany(target, objectToWire);
      addManyToMany(objectToWire, target);
      return toBePersisted;
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
}
