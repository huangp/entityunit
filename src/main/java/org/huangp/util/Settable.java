package org.huangp.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
// TODO remove the need of extending AnnotatedElement
public interface Settable extends AnnotatedElement
{
   String getSimpleName();

   Type getType();

   String getterMethodName();

   String setterMethodName();
}
