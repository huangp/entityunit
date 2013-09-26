package com.github.huangp.entityunit.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * A adaptor of field, property or constructor parameter.
 *
 * @author Patrick Huang
 */
public interface Settable extends AnnotatedElement {
    String FULL_NAME_FORMAT = "%s - %s";

    /**
     * For field/property is the field name,
     * <p/>
     * For constructor it's arg# where # is the index of the argument in constructor method.
     *
     * @return simple name of this settable.
     */
    String getSimpleName();

    /**
     * @return type of this settable
     */
    Type getType();

    /**
     * When settable represents a field or property, this returns the getter method.
     *
     * @return getter method of the field/property
     */
    Method getterMethod();

    /**
     * Use owner_class_name - simpleName as format.
     * <p/>
     * For settable field, property, i.e. org.example.Person - name.
     * <p/>
     * For settable constructor parameter, simple name is arg# where # is the index of argument,
     * i.e. org.example.Locale - arg0
     *
     * @return owner_class_name - simpleName
     * @see Settable#FULL_NAME_FORMAT
     * @see com.github.huangp.entityunit.maker.PreferredValueMakersRegistry
     */
    String fullyQualifiedName();
}
