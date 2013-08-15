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

   private SettableParameter(Parameter parameter)
   {
      this.parameter = parameter;
   }

   public static Settable from(Parameter parameter)
   {
      return new SettableParameter(parameter);
   }

   @Override
   public Type getType()
   {
      return parameter.getType().getType();
   }

   @Override
   public String getSimpleName()
   {
      return parameter.toString();
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
      return parameter.toString();
   }
}
