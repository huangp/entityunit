package org.huangp.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import com.google.common.reflect.Parameter;

import lombok.Delegate;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@RequiredArgsConstructor
public class SettableParameter implements Settable
{
   @Delegate(types = AnnotatedElement.class)
   private final Parameter parameter;

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
}
