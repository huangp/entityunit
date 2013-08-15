package org.huangp.makeit.maker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.huangp.makeit.holder.BeanValueHolder;
import org.huangp.makeit.holder.BeanValueHolderImpl;
import org.huangp.makeit.scanner.EntityClassImpl;
import org.huangp.makeit.util.ClassUtil;
import org.huangp.makeit.util.Settable;
import org.huangp.makeit.util.SettableParameter;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class SingleEntityMaker<T> implements Maker<T>
{
   private static final BeanValueHolder holder = BeanValueHolderImpl.HOLDER;

   private final Class<T> entityType;

   public SingleEntityMaker(Class<T> entityType)
   {
      this.entityType = entityType;
   }

   @Override
   public T value()
   {
      T result = null;
      try
      {
         Invokable<T, T> constructor = ClassUtil.findMostArgsConstructor(entityType);
         result = constructEntity(result, constructor);

         // if we can find public static constants defined in the class, we will use that as value
         Optional<T> constants = ClassUtil.tryFindPublicConstants(entityType, result);
         if (constants.isPresent())
         {
            return constants.get();
         }
         // populate all settable fields
         return setApplicableFields(result);
      }
      catch (Exception e)
      {
         throw Throwables.propagate(e);
      }
      finally
      {
         log.debug("entity of type {} made: {}", entityType, result);
         holder.putIfNotNull(entityType, result);
      }
   }

   private T constructEntity(T result, Invokable<T, T> constructor)
         throws InvocationTargetException, IllegalAccessException, InstantiationException
   {
      ImmutableList<Parameter> parameters = constructor.getParameters();
      // TODO this may override some default values provided at field declaration. See HCopyTransOptions
      List<Object> paramValues = Lists.transform(parameters, new Function<Parameter, Object>()
      {
         @Override
         public Object apply(Parameter input)
         {
            Settable settableParameter = SettableParameter.from(input);
            return ScalarValueMakerFactory.FACTORY.from(settableParameter).value();
         }
      });
      try
      {
         log.debug("invoke args {} constructor with parameters {}", entityType, parameters);
         constructor.setAccessible(true);
         return constructor.invoke(result, paramValues.toArray());
      }
      catch (Exception e)
      {
         log.warn("fail calling constructor method: {}. Will fall back to default constructor", constructor);
         Invokable<T, T> noArgConstructor = ClassUtil.getNoArgConstructor(entityType);
         noArgConstructor.setAccessible(true);
         return noArgConstructor.invoke(result);
      }
   }

   private T setApplicableFields(T result) throws InvocationTargetException, IllegalAccessException
   {
      // we want every field or properties
      Iterable<Settable> elements = EntityClassImpl.from(entityType).getElements();

      Predicate<Settable> settableFieldPredicate = not(
            or(new SameTypePredicate(result.getClass()), new DoNotNeedValuePredicate<T>(result)));

      Iterable<Settable> fieldsToSet = Iterables.filter(elements, settableFieldPredicate);
      for (Settable settable : fieldsToSet)
      {
         tryPopulatePropertyValue(result, settable);
      }
      return result;

   }

   private void tryPopulatePropertyValue(T result, Settable settable)
   {
      log.debug("about to make {}.{}", entityType.getSimpleName(), settable.getSimpleName());
      Object fieldValue = ScalarValueMakerFactory.FACTORY.from(settable).value();
      log.debug("value {}", fieldValue);
      try
      {
         BeanUtils.setProperty(result, settable.getSimpleName(), fieldValue);
      }
      catch (Exception e)
      {
         log.warn("can not set property: {}={}", settable.getSimpleName(), fieldValue);
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
   private static class DoNotNeedValuePredicate<T> implements Predicate<Settable>
   {
      private final T object;

      @Override
      public boolean apply(Settable input)
      {
         try
         {
            Method getter = object.getClass().getMethod(input.getterMethodName());
            return getter.getReturnType().isPrimitive() || getter.invoke(object) != null;
         }
         catch (IllegalAccessException e)
         {
            log.warn("can not determine field has default value or not");
            return true;
         } 
         catch (NoSuchMethodException e)
         {
            log.warn("getter method not found {}", e.getMessage());
            return true;
         } 
         catch (InvocationTargetException e)
         {
            log.warn("can not invoke getter method {}", e.getMessage());
            return true;
         }
      }
   }

}
