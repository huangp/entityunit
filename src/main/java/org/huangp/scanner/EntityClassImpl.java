package org.huangp.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.huangp.util.ClassUtil;
import org.huangp.util.Settable;
import org.huangp.util.SettableField;
import org.huangp.util.SettableProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static com.google.common.collect.Iterables.filter;
import static org.huangp.scanner.EntityClassImpl.HasAnnotationPredicate.has;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class EntityClassImpl implements EntityClass
{
   private final Predicate<AnnotatedElement> requiredOneToOnePredicate = Predicates.and(has(OneToOne.class), RequiredOneToOnePredicate.PREDICATE);
   @Getter
   private final Class type;
   @Getter
   private boolean scanned;
   @Getter
   private final Iterable<Settable> elements;

   private transient Iterable<Class<?>> dependingTypes;
   private transient Iterable<Method> associationGetters;
   private transient List<Method> allDeclaredMethods;

   private EntityClassImpl(Class type, Iterable<Settable> elements)
   {
      this.type = type;
      allDeclaredMethods = ClassUtil.getAllDeclaredMethods(type);
      List<Settable> settables = Lists.newArrayList(elements);
      Collections.sort(settables, NameComparator.COMPARATOR);
      this.elements = ImmutableList.copyOf(settables);
   }

   public static EntityClass from(final Class clazz)
   {
      List<Field> allInstanceFields = ClassUtil.getAllInstanceFields(clazz);
      List<Settable> settableFields = Lists.transform(allInstanceFields, FieldToSettableFunction.FUNCTION);
      if (ClassUtil.isAccessTypeIsField(clazz))
      {
         // field based annotation
         return new EntityClassImpl(clazz, settableFields);
      }
      else
      {
         // property based annotation
         List<Method> allMethods = ClassUtil.getAllDeclaredMethods(clazz);
         // find all getter methods
         Iterable<Method> methods = Iterables.filter(allMethods, new Predicate<Method>()
         {
            @Override
            public boolean apply(Method input)
            {
               return input.getName().matches("^(is|get)\\w+");
            }
         });
         List<Settable> settableMethods = Lists.newArrayList(Iterables.transform(methods, MethodToSettableFunction.FUNCTION));
         return new EntityClassImpl(clazz, settableMethods);
      }
   }

   @Override
   public EntityClassImpl markScanned()
   {
      scanned = true;
      return this;
   }

   @Override
   public Iterable<Class<?>> getDependingEntityTypes()
   {
      if (dependingTypes == null)
      {
         Iterable<Settable> manyToOne = Iterables.filter(elements, Predicates.or(has(ManyToOne.class), requiredOneToOnePredicate));
         dependingTypes = Iterables.transform(manyToOne, TypeFunction.FUNCTION);
      }
      return dependingTypes;
   }

   @Override
   public Iterable<Method> getContainingEntitiesGetterMethods()
   {
      if (associationGetters == null)
      {
         Iterable<Settable> oneToMany = Iterables.filter(elements, has(OneToMany.class));
         List<String> methodNames = Lists.newArrayList(Iterables.transform(oneToMany, SettableGetterMethodFunction.FUNCTION));
         associationGetters = filter(allDeclaredMethods, new MethodNameMatchPredicate(methodNames));
      }
      return associationGetters;
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

   private static enum SettableGetterMethodFunction implements Function<Settable, String>
   {
      FUNCTION;
      @Override
      public String apply(Settable input)
      {
         return input.getterMethodName();
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

   private static class MethodNameMatchPredicate implements Predicate<Method>
   {
      private final List<String> methodNames;

      public MethodNameMatchPredicate(List<String> methodNames)
      {
         this.methodNames = methodNames;
      }

      @Override
      public boolean apply(Method input)
      {
         return methodNames.contains(input.getName());
      }
   }

   private static enum MethodToSettableFunction implements Function<Method, Settable>
   {
      FUNCTION;

      @Override
      public Settable apply(Method input)
      {
         return new SettableProperty(input);
      }
   }

   private static enum FieldToSettableFunction implements Function<Field, Settable>
   {
      FUNCTION;

      @Override
      public Settable apply(Field input)
      {
         return new SettableField(input);
      }
   }

   /**
   * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
   */
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
}
