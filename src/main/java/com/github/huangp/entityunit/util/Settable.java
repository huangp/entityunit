package com.github.huangp.entityunit.util;

import com.github.huangp.entityunit.maker.PreferredValueMakersRegistry;

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
     * <p>
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
     * Use owner_class_name - simpleName as format.
     * <p>
     * For settable field, property, i.e. org.example.Person - name.
     * <p>
     * For settable constructor parameter, simple name is arg# where # is the index of argument,
     * i.e. org.example.Locale - arg0
     *
     * @return owner_class_name - simpleName
     * @see Settable#FULL_NAME_FORMAT
     * @see PreferredValueMakersRegistry
     */
    String fullyQualifiedName();

    /**
     * Actual value in given instance. Like java.lang.reflect.Field#get(java.lang.Object)
     * or getter method java.lang.reflect.Method#invoke(java.lang.Object, java.lang.Object...).
     *
     * @param ownerInstance
     *         instance that contains this Settable
     * @return instance value of this Settable
     */
    <T> T valueIn(Object ownerInstance);
}
