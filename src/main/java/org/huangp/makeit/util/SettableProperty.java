package org.huangp.makeit.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

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
   private transient final String fullName;

   private SettableProperty(Class ownerType, Method method)
   {
      this.method = method;
      setterMethodName = method.getName().replaceFirst("get|is", "set");
      String stripped = method.getName().replaceFirst("get|is", "");
      String lower = stripped.substring(0, 1).toLowerCase();
      String rest = stripped.substring(1);
      simpleName = lower + rest;
      fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), simpleName);
   }
   public static Settable from(Class ownerType, Method method)
   {
      return new SettableProperty(ownerType, method);
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
      return setterMethodName;
   }

   @Override
   public String fullyQualifiedName()
   {
      return fullName;
   }

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .add("simpleName", simpleName)
            .add("setterMethodName", setterMethodName)
            .toString();
   }

}
