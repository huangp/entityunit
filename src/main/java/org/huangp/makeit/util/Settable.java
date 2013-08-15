package org.huangp.makeit.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface Settable extends AnnotatedElement
{
   String getSimpleName();

   Type getType();

   String getterMethodName();

   String setterMethodName();

   String fullyQualifiedName();
}
