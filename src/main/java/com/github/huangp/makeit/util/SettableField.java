package com.github.huangp.makeit.util;

import java.lang.reflect.Field;

import com.google.common.base.Objects;

import lombok.Delegate;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class SettableField implements Settable
{
   @Delegate
   private final Field field;
   private transient final String getter;
   private transient final String setter;
   private transient final String fullName;

   private SettableField(Class ownerType, Field field)
   {
      this.field = field;
      String capitalized = capitalizeFieldName(field.getName());
      if (field.getType().isPrimitive() && field.getType().equals(boolean.class))
      {
         getter = "is" + capitalized;
      }
      else
      {
         getter = "get" + capitalized;
      }
      setter = "set" + capitalized;
      fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), field.getName());
   }

   public static Settable from(Class ownerType, Field field)
   {
      return new SettableField(ownerType, field);
   }

   private static String capitalizeFieldName(String simpleName)
   {
      String cap = simpleName.substring(0, 1).toUpperCase();
      String rest = simpleName.substring(1);
      return cap + rest;
   }

   @Override
   public String getSimpleName()
   {
      return field.getName();
   }

   @Override
   public String getterMethodName()
   {
      return getter;
   }

   @Override
   public String setterMethodName()
   {
      return setter;
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
            .add("field", field)
            .add("getter", getter)
            .add("setter", setter)
            .toString();
   }
}
