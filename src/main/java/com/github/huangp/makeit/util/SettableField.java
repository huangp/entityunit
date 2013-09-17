package com.github.huangp.makeit.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.Delegate;

/**
 * @author Patrick Huang
 */
public class SettableField implements Settable
{
   @Delegate
   private final Field field;
   private final Method getterMethod;
   private transient final String fullName;

   private SettableField(Class ownerType, Field field)
   {
      this.field = field;
      getterMethod = ClassUtil.getterMethod(ownerType, field.getName());
      fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), field.getName());
   }

   @Override
   public Method getterMethod()
   {
      return getterMethod;
   }

   public static Settable from(Class ownerType, Field field)
   {
      return new SettableField(ownerType, field);
   }

   @Override
   public String getSimpleName()
   {
      return field.getName();
   }

   @Override
   public String fullyQualifiedName()
   {
      return fullName;
   }

   @Override
   public String toString()
   {
      return fullName;
   }
}
