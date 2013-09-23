package com.github.huangp.entityunit.util;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang
 */
public class SettableProperty implements Settable {
    private final Optional<Field> optionalField;
    private final Method getterMethod;

    private transient final String simpleName;
    private transient final String fullName;
    private transient final Type propertyType;

    private SettableProperty(Class ownerType, PropertyDescriptor propertyDescriptor) {
        simpleName = propertyDescriptor.getName();
        fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), simpleName);

        optionalField = findField(ownerType, simpleName);

        getterMethod = propertyDescriptor.getReadMethod();
        propertyType = propertyDescriptor.getPropertyType();
    }

    private static Optional<Field> findField(Class ownerType, final String fieldName) {
        return Iterables.tryFind(ClassUtil.getAllDeclaredFields(ownerType), new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return input.getName().equals(fieldName);
            }
        });
    }

    private SettableProperty(Class ownerType, Method getterMethod) {
        this.getterMethod = getterMethod;
        String stripped = getterMethod.getName().replaceFirst("get|is", "");
        String lower = stripped.substring(0, 1).toLowerCase();
        String rest = stripped.substring(1);
        simpleName = lower + rest;
        optionalField = findField(ownerType, simpleName);
        propertyType = getterMethod.getGenericReturnType();
        fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), simpleName);
    }

    public static Settable from(Class ownerType, PropertyDescriptor propertyDescriptor) {
        return new SettableProperty(ownerType, propertyDescriptor);
    }

    public static Settable from(Class ownerType, Method getterMethod) {
        return new SettableProperty(ownerType, getterMethod);
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public Type getType() {
        return propertyType;
    }

    @Override
    public Method getterMethod() {
        return getterMethod;
    }

    @Override
    public String fullyQualifiedName() {
        return fullName;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return (optionalField.isPresent() && optionalField.get().isAnnotationPresent(annotationClass))
                || getterMethod.isAnnotationPresent(annotationClass);

    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T annotation = null;
        if (optionalField.isPresent()) {
            annotation = optionalField.get().getAnnotation(annotationClass);
        }
        return annotation != null ? annotation : getterMethod.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        if (optionalField.isPresent()) {
            return ObjectArrays.concat(optionalField.get().getAnnotations(), getterMethod.getAnnotations(), Annotation.class);
        }
        return getterMethod.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        if (optionalField.isPresent()) {
            return ObjectArrays.concat(optionalField.get().getDeclaredAnnotations(), getterMethod.getDeclaredAnnotations(), Annotation.class);
        }
        return getterMethod.getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return fullName;
    }
}
