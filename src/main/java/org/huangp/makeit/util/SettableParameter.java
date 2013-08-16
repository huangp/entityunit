package org.huangp.makeit.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import com.google.common.reflect.Parameter;

import lombok.Delegate;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class SettableParameter implements Settable
{
   @Delegate(types = AnnotatedElement.class)
   private final Parameter parameter;
   private transient final String simpleName;
   private transient final String fullName;

   private SettableParameter(Class<?> ownerType, Parameter parameter)
   {
      simpleName = parameter.toString().replaceFirst("^.+\\s", "");
      this.fullName = ownerType.getName() + "(" + simpleName + ")";
      this.parameter = parameter;
   }

   public static Settable from(Class<?> ownerType, Parameter parameter)
   {
      return new SettableParameter(ownerType, parameter);
   }

   @Override
   public Type getType()
   {
      return parameter.getType().getType();
   }

   @Override
   public String getSimpleName()
   {
      return simpleName;
   }

   @Override
   public String getterMethodName()
   {
      throw new UnsupportedOperationException("This should not be called");
   }

   @Override
   public String setterMethodName()
   {
      throw new UnsupportedOperationException("This should not be called");
   }

   @Override
   public String fullyQualifiedName()
   {
      return fullName;
   }
}
