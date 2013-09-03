package com.github.huangp.makeit.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.common.base.Objects;

import lombok.Delegate;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class SettableProperty implements Settable
{
   private final Class ownerType;
   @Delegate
   private final Method method;

   private transient final String simpleName;
   private transient final String fullName;

   private SettableProperty(Class ownerType, Method method)
   {
      this.ownerType = ownerType;
      this.method = method;
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
   public Method getterMethod()
   {
      return method;
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
   public String fullyQualifiedName()
   {
      return fullName;
   }

   @Override
   public String toString()
   {
      return ownerType.getSimpleName() + "." + getSimpleName();
   }

}
