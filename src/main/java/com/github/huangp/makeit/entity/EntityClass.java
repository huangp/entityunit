package com.github.huangp.makeit.entity;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.github.huangp.makeit.util.ClassUtil;
import com.github.huangp.makeit.util.Settable;
import com.github.huangp.makeit.util.SettableField;
import com.github.huangp.makeit.util.SettableProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import static com.github.huangp.makeit.entity.EntityClass.HasAnnotationPredicate.has;
import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@ToString(of = "type")
public class EntityClass
{
   private static Cache<CacheKey, EntityClass> cache = CacheBuilder.newBuilder()
         .maximumSize(100)
         .build();

   private Predicate<AnnotatedElement> oneToOnePredicate;
   @Getter
   private final Class type;
   @Getter
   private final Iterable<Settable> elements;

   private transient Iterable<Class<?>> dependingTypes;
   private transient Iterable<Method> associationGetters;
   private transient Iterable<String> manyToManyTables;
   private transient Iterable<Method> manyToManyGetters;

   private EntityClass(Class type, Iterable<Settable> elements, ScanOption scanOption)
   {
      this.type = type;
      List<Settable> settables = newArrayList(elements);
      Collections.sort(settables, NameComparator.COMPARATOR);
      this.elements = ImmutableList.copyOf(settables);
      if (scanOption == ScanOption.IgnoreOptionalOneToOne)
      {
         oneToOnePredicate = Predicates.and(has(OneToOne.class), RequiredOneToOnePredicate.PREDICATE);
      }
      else
      {
         oneToOnePredicate = Predicates.and(has(OneToOne.class), has(JoinColumn.class));
      }
   }

   public static EntityClass from(final Class clazz)
   {
      return from(clazz, ScanOption.IgnoreOptionalOneToOne);
   }

   /**
    * Factory method.
    *
    * @param clazz the class to wrap
    * @param scanOption whether consider optional OneToOne as required
    * @return a wrapper for the entity class
    */
   public static EntityClass from(final Class clazz, final ScanOption scanOption)
   {

      try
      {
         return cache.get(CacheKey.of(clazz, scanOption), new Callable<EntityClass>()
         {
            @Override
            public EntityClass call() throws Exception
            {
               return createEntityClass(clazz, scanOption);
            }
         });
      }
      catch (ExecutionException e)
      {
         throw Throwables.propagate(e);
      }
   }

   private static EntityClass createEntityClass(Class clazz, ScanOption scanOption)
   {
      if (ClassUtil.isAccessTypeIsField(clazz))
      {
         List<Field> allInstanceFields = ClassUtil.getAllInstanceFields(clazz);
         List<Settable> settableFields = Lists.transform(allInstanceFields, new FieldToSettableFunction(clazz));
         // field based annotation
         return new EntityClass(clazz, settableFields, scanOption);
      }
      else
      {
         // property based annotation
         Iterable<Method> allMethods = ClassUtil.getAllPropertyReadMethods(clazz);
         List<Settable> settableMethods = newArrayList(Iterables.transform(allMethods, new MethodToSettableFunction(clazz)));
         return new EntityClass(clazz, settableMethods, scanOption);
      }
   }

   public Iterable<Class<?>> getDependingEntityTypes()
   {
      if (dependingTypes == null)
      {
         Iterable<Settable> manyToOne = Iterables.filter(elements, Predicates.or(has(ManyToOne.class), oneToOnePredicate));
         dependingTypes = Iterables.transform(manyToOne, TypeFunction.FUNCTION);
      }
      return dependingTypes;
   }

   public Iterable<Method> getContainingEntitiesGetterMethods()
   {
      if (associationGetters == null)
      {
         Iterable<Settable> oneToMany = Iterables.filter(elements, has(OneToMany.class));
         associationGetters = Iterables.transform(oneToMany, SettableGetterMethodFunction.FUNCTION);
      }
      return associationGetters;
   }

   public Iterable<String> getManyToManyTables()
   {
      if (manyToManyTables == null)
      {
         Iterable<Settable> manyToMany = Iterables.filter(elements, Predicates.and(has(ManyToMany.class), has(JoinTable.class)));
         manyToManyTables = Iterables.transform(manyToMany, new Function<Settable, String>()
         {
            @Override
            public String apply(Settable input)
            {
               JoinTable annotation = input.getAnnotation(JoinTable.class);
               return annotation.name();
            }
         });

      }
      return manyToManyTables;
   }

   public Iterable<Method> getManyToManyMethods()
   {
      if (manyToManyGetters == null)
      {
         Iterable<Settable> manyToMany = Iterables.filter(elements, Predicates.and(has(ManyToMany.class), has(JoinTable.class)));
         manyToManyGetters = Iterables.transform(manyToMany, SettableGetterMethodFunction.FUNCTION);
      }
      return manyToManyGetters;
   }

   private static enum NameComparator implements Comparator<Settable>
   {
      COMPARATOR;
      @Override
      public int compare(Settable o1, Settable o2)
      {
         return o1.getSimpleName().compareTo(o2.getSimpleName());
      }
   }

   private static enum TypeFunction implements Function<Settable, Class<?>>
   {
      FUNCTION;
      @Override
      public Class<?> apply(Settable input)
      {
         return TypeToken.of(input.getType()).getRawType();
      }
   }

   private static enum SettableGetterMethodFunction implements Function<Settable, Method>
   {
      FUNCTION;
      @Override
      public Method apply(Settable input)
      {
         return input.getterMethod();
      }
   }

   private static enum RequiredOneToOnePredicate implements Predicate<AnnotatedElement>
   {
      PREDICATE;

      @Override
      public boolean apply(AnnotatedElement input)
      {
         OneToOne oneToOne = input.getAnnotation(OneToOne.class);
         return oneToOne != null && !oneToOne.optional();
      }
   }

   @RequiredArgsConstructor
   private static class MethodToSettableFunction implements Function<Method, Settable>
   {
      private final Class ownerType;

      @Override
      public Settable apply(Method input)
      {
         return SettableProperty.from(ownerType, input);
      }
   }

   @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
   static class HasAnnotationPredicate<A extends AnnotatedElement> implements Predicate<A>
   {
      private final Class<? extends Annotation> annotationClass;

      public static <A extends AnnotatedElement> Predicate<A> has(Class<? extends Annotation> annotationClass)
      {
         return new HasAnnotationPredicate<A>(annotationClass);
      }

      @Override
      public boolean apply(A input)
      {
         return input.isAnnotationPresent(annotationClass);
      }
   }

   @RequiredArgsConstructor
   private static class FieldToSettableFunction implements Function<Field, Settable>
   {
      private final Class ownerType;

      @Override
      public Settable apply(Field input)
      {
         return SettableField.from(ownerType, input);
      }
   }
}
