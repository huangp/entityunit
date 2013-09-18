package com.github.huangp.entityunit.maker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import com.github.huangp.entityunit.entity.EntityClass;
import com.github.huangp.entityunit.entity.MakeContext;
import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.Settable;
import com.github.huangp.entityunit.util.SettableParameter;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang
 */
@Slf4j
public class BeanMaker<T> implements Maker<T>
{
   private final Class<T> type;
   private final MakeContext context;
   private final ScalarValueMakerFactory factory;

   public BeanMaker(Class<T> type, MakeContext context)
   {
      this.type = type;
      this.context = context;
      factory = new ScalarValueMakerFactory(context);
   }

   @Override
   public T value()
   {
      T result = null;
      try
      {
         Invokable<T, T> constructor = ClassUtil.findMostArgsConstructor(type);
         result = constructBean(result, constructor);

         // if we can find public static constants defined in the class, we will use that as value
         Optional<T> constants = ClassUtil.tryFindPublicConstants(type, result);
         if (constants.isPresent())
         {
            return constants.get();
         }

         // populate all fields
         return setApplicableFields(result);
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
      finally
      {
         log.debug("bean of type {} made: {}", type, result);
         context.getBeanValueHolder().putIfNotNull(type, result);
      }
   }

   private T constructBean(T result, Invokable<T, T> constructor)
         throws InvocationTargetException, IllegalAccessException, InstantiationException
   {
      ImmutableList<Parameter> parameters = constructor.getParameters();
      // TODO this may override some default values provided at field declaration. See HCopyTransOptions
      List<Object> paramValues = Lists.transform(parameters, new Function<Parameter, Object>()
      {
         @Override
         public Object apply(Parameter input)
         {
            Settable settableParameter = SettableParameter.from(type, input);
            return factory.from(settableParameter).value();
         }
      });
      try
      {
         log.debug("invoke args {} constructor with parameters {}", type, parameters);
         constructor.setAccessible(true);
         return constructor.invoke(result, paramValues.toArray());
      }
      catch (Exception e)
      {
         log.warn("fail calling constructor method: {}. Will fall back to default constructor", constructor);
         log.warn("exception", e);
         Invokable<T, T> noArgConstructor = ClassUtil.getNoArgConstructor(type);
         noArgConstructor.setAccessible(true);
         return noArgConstructor.invoke(result);
      }
   }

   private T setApplicableFields(T result) throws InvocationTargetException, IllegalAccessException
   {
      Iterable<Settable> elements = EntityClass.from(type).getElements();

      Predicate<Settable> settablePredicate = Predicates.not(
            Predicates.or(new SameTypePredicate(result.getClass()), new HasDefaultValuePredicate<T>(result)));

      Iterable<Settable> fieldsToSet = Iterables.filter(elements, settablePredicate);
      for (Settable settable : fieldsToSet)
      {
         tryPopulatePropertyValue(result, settable);
      }
      return result;

   }

   private void tryPopulatePropertyValue(T result, Settable settable)
   {
      log.debug("about to make {}", settable);
      Object fieldValue = factory.from(settable).value();
      log.debug("value {}", fieldValue);
      try
      {
         BeanUtils.setProperty(result, settable.getSimpleName(), fieldValue);
         if (log.isDebugEnabled())
         {
            log.debug("value after set: {}", settable.getterMethod().invoke(result));
         }
      }
      catch (Exception e)
      {
         log.warn("can not set property: {}={}", settable, fieldValue);
         log.warn("exception", e);
      }
   }

   @RequiredArgsConstructor
   private static class SameTypePredicate implements Predicate<Settable>
   {
      private final Class type;

      @Override
      public boolean apply(Settable input)
      {
         return input.getType().equals(type);
      }
   }

   @RequiredArgsConstructor
   private static class HasDefaultValuePredicate<T> implements Predicate<Settable>
   {
      private final T object;

      @Override
      public boolean apply(Settable input)
      {
         try
         {
            Method getter = input.getterMethod();
            return notPrimitive(getter) && getter.invoke(object) != null;
         }
         catch (IllegalAccessException e)
         {
            log.warn("can not determine field {} has default value or not", input);
            return false;
         }
         catch (InvocationTargetException e)
         {
            log.warn("can not invoke getter method {}", e.getMessage());
            return false;
         }
      }

      private static boolean notPrimitive(Method getter)
      {
         return !getter.getReturnType().isPrimitive();
      }
   }
}
