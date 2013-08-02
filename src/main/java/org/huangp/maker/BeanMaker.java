package org.huangp.maker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.huangp.holder.BeanValueHolder;
import org.huangp.holder.BeanValueHolderImpl;
import org.huangp.util.ClassUtil;
import org.huangp.util.SettableField;
import org.huangp.util.SettableParameter;

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
import com.google.common.reflect.TypeToken;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class BeanMaker<T> extends AbstractMaker<T>
{
   private static final BeanValueHolder holder = BeanValueHolderImpl.HOLDER;

   private final Class<T> type;

   BeanMaker(Class<T> type)
   {
      this.type = type;
   }

   @Override
   public T value()
   {
      T result = null;
      try
      {
         Invokable<T, T> constructor = ClassUtil.findMostArgsConstructor(type);
         constructor.setAccessible(true);

         ImmutableList<Parameter> parameters = constructor.getParameters();
//         log.debug("invoke args constructor with parameters {}", parameters);
         // TODO this may override some default values provided at field initialization. See HCopyTransOptions
         List<Object> paramValues = Lists.transform(parameters, new Function<Parameter, Object>()
         {
            @Override
            public Object apply(Parameter input)
            {
               return getExistingOrMakeNew(input);
            }
         });
         result = constructor.invoke(result, paramValues.toArray());

         // if we can find public static constants defined in the class, we will use that as value
         Optional<T> constants = ClassUtil.tryFindPublicConstants(type, result);
         if (constants.isPresent())
         {
            holder.putIfNotNull(type, constants.get());
            return constants.get();
         }

         // populate all fields
         holder.putIfNotNull(type, result);
         return makeAllFields(result);
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
   }

   private T makeAllFields(T result) throws InvocationTargetException, IllegalAccessException
   {
      List<Field> fields = ClassUtil.getAllInstanceFields(type);

      Predicate<Field> settablePredicate = Predicates.not(
            Predicates.or(new SameTypePredicate(result.getClass()), new HasDefaultValuePredicate<T>(result)));

      Iterable<Field> fieldsToSet = Iterables.filter(fields, settablePredicate);
      for (Field field : fieldsToSet)
      {
         log.debug("about to make {}.{}", type.getSimpleName(), field.getName());
         Object fieldValue = getExistingOrMakeNew(field);
         BeanUtils.setProperty(result, field.getName(), fieldValue);
      }
      return result;

   }

   private static Object getExistingOrMakeNew(Parameter parameter)
   {
      Type type = parameter.getType().getType();
      Optional<?> existingValue = holder.tryGet(TypeToken.of(type));
      SettableParameter settableParameter = new SettableParameter(parameter);
      return existingValue.isPresent() ? existingValue.get() : ScalarValueMakerFactory.FACTORY.from(settableParameter).value();
   }

   private static Object getExistingOrMakeNew(Field field)
   {
      Type type = field.getGenericType();
      Optional<?> existingValue = holder.tryGet(TypeToken.of(type));
      // TODO pahuang check validation annotation when invoking factory
      return existingValue.isPresent() ? existingValue.get() : ScalarValueMakerFactory.FACTORY.from(new SettableField(field)).value();
   }

   @RequiredArgsConstructor
   private static class SameTypePredicate implements Predicate<Field>
   {
      private final Class type;

      @Override
      public boolean apply(Field input)
      {
         return input.getType().equals(type);
      }
   }

   @RequiredArgsConstructor
   private static class HasDefaultValuePredicate<T> implements Predicate<Field>
   {
      private final T object;

      @Override
      public boolean apply(Field input)
      {
         try
         {
            input.setAccessible(true);
            return input.getType().isPrimitive() || input.get(object) != null;
         }
         catch (IllegalAccessException e)
         {
            return false;
         }
      }
   }
}
