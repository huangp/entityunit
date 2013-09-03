package com.github.huangp.makeit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public final class ClassUtil
{
   private ClassUtil()
   {
   }

   /**
    * Return all non-static and non-transient fields.
    *
    * @param type class to work with
    * @return list of fields
    */
   public static List<Field> getAllInstanceFields(Class type)
   {
      List<Field> fields = Lists.newArrayList(type.getDeclaredFields());

      Class<?> superClass = type.getSuperclass();
      while (superClass != null)
      {
         fields.addAll(Lists.newArrayList(superClass.getDeclaredFields()));
         superClass = superClass.getSuperclass();
      }
      return ImmutableList.copyOf(Iterables.filter(fields, new Predicate<Field>()
      {
         @Override
         public boolean apply(Field input)
         {
            int mod = input.getModifiers();
            return !Modifier.isStatic(mod) && !Modifier.isTransient(mod);
         }
      }));
   }

   public static List<Method> getAllDeclaredMethods(Class clazz)
   {
      List<Method> methods = Lists.newArrayList(clazz.getDeclaredMethods());

      Class<?> superClass = clazz.getSuperclass();
      while (superClass != null && superClass != Object.class)
      {
         methods.addAll(Lists.newArrayList(superClass.getDeclaredMethods()));
         superClass = superClass.getSuperclass();
      }
      return ImmutableList.copyOf(methods);
   }

   public static <T> Optional<T> tryFindPublicConstants(final Class<T> type, T instance) throws IllegalAccessException
   {
      List<Field> fields = Lists.newArrayList(type.getDeclaredFields());
      Optional<Field> found = Iterables.tryFind(fields, new Predicate<Field>()
      {
         @Override
         public boolean apply(Field input)
         {
            int mod = input.getModifiers();
            Class<?> fieldType = input.getType();
            return fieldType.equals(type) && Modifier.isPublic(mod) && Modifier.isStatic(mod);
         }
      });
      if (found.isPresent())
      {
         return Optional.of((T) found.get().get(instance));
      }
      return Optional.absent();
   }

   public static <T> Invokable<T, T> findMostArgsConstructor(Class<T> type)
   {
      List<Constructor<?>> constructors = Lists.newArrayList(type.getDeclaredConstructors());

      // sort by number of parameters in descending order
      Collections.sort(constructors, new Comparator<Constructor<?>>()
      {
         @Override
         public int compare(Constructor<?> o1, Constructor<?> o2)
         {
            return o2.getParameterTypes().length - o1.getParameterTypes().length;
         }
      });

      return (Invokable<T, T>) Invokable.from(constructors.get(0));
   }

   public static <T> Invokable<T, T> getNoArgConstructor(Class<T> entityType)
   {
      try
      {
         return Invokable.from(entityType.getConstructor());
      }
      catch (NoSuchMethodException e)
      {
         throw Throwables.propagate(e);
      }
   }

   public static boolean isAccessTypeIsField(Class clazz)
   {
      Annotation access = clazz.getAnnotation(Access.class);
      if (access != null)
      {
         AccessType accessType = ((Access) access).value();
         return accessType == AccessType.FIELD;
      }
      return false;
   }

   public static boolean isCollection(Type type)
   {
      return Collection.class.isAssignableFrom(TypeToken.of(type).getRawType());
   }

   public static boolean isMap(Type type)
   {
      return Map.class.isAssignableFrom(TypeToken.of(type).getRawType());
   }

   public static boolean isEntity(Type type)
   {
      return TypeToken.of(type).getRawType().isAnnotationPresent(Entity.class);
   }

   public static <T> T findEntity(Iterable<Object> entities, Class<T> typeToFind)
   {
      return (T) Iterables.find(entities, Predicates.instanceOf(typeToFind));
   }

   public static <T> T invokeGetter(Object entity, Method method, Class<T> getterReturnType)
   {
      try
      {
         T result = (T) method.invoke(entity);
         return result;
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
   }
}
