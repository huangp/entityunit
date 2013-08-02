package org.huangp.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.common.base.Function;
import com.google.common.base.Objects;

import lombok.Delegate;
import lombok.extern.slf4j.Slf4j;
import static com.google.common.collect.Iterables.filter;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class SettableProperty implements Settable
{
   @Delegate
   private final Method method;
   private transient final String setterMethodName;
   private transient final String simpleName;

   public SettableProperty(Method method)
   {
      this.method = method;
      setterMethodName = GetterToSetterNameFunction.FUNCTION.apply(method);
      String stripped = method.getName().replaceFirst("get", "");
      String lower = stripped.substring(0, 1).toLowerCase();
      String rest = stripped.substring(1);
      simpleName = lower + rest;
   }

   @Override
   public String getSimpleName()
   {
      return simpleName;
   }

   @Override
   public Type getType()
   {
      return method.getGenericReturnType();
   }

   @Override
   public String getterMethodName()
   {
      return method.getName();
   }

   @Override
   public String setterMethodName()
   {
      return method.getName().replaceFirst("get", "set");
   }

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .add("simpleName", simpleName)
            .add("setterMethodName", setterMethodName)
            .toString();
   }

   private static enum GetterToSetterNameFunction implements Function<Method, String>
   {
      FUNCTION;

      @Override
      public String apply(Method input)
      {
         return input.getName().replaceFirst("get", "set");
      }
   }
}
