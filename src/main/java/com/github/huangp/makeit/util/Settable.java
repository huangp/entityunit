package com.github.huangp.makeit.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface Settable extends AnnotatedElement
{
   String FULL_NAME_FORMAT = "%s - %s";

   String getSimpleName();

   Type getType();

   String getterMethodName();

   String setterMethodName();

   /**
    * Use owner_class_name - simpleName as format.
    * For settable field, property, i.e. org.example.Person - name.
    * For settable constructor parameter, simple name is arg# where # is the index of argument,
    * i.e. org.example.Locale - arg0
    *
    * @see Settable#FULL_NAME_FORMAT
    * @see com.github.huangp.makeit.maker.PreferredValueMakersRegistry
    *
    * @return owner_class_name - simpleName
    */
   String fullyQualifiedName();
}
