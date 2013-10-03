package com.github.huangp.entityunit.util;

import lombok.Delegate;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang
 */
public class SettableField implements Settable {
    @Delegate(types = AnnotatedElement.class)
    private final Field field;
    private final Method getterMethod;
    private final transient String fullName;

    private SettableField(Class ownerType, Field field) {
        this.field = field;
        getterMethod = ClassUtil.getterMethod(ownerType, field.getName());
        fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), field.getName());
    }

    public static Settable from(Class ownerType, Field field) {
        return new SettableField(ownerType, field);
    }

    @Override
    public Type getType() {
        return field.getGenericType();
    }

    @Override
    public String getSimpleName() {
        return field.getName();
    }

    @Override
    public String fullyQualifiedName() {
        return fullName;
    }

    @Override
    public <T> T valueIn(Object ownerInstance) {
        if (getterMethod != null) {
            return ClassUtil.invokeGetter(ownerInstance, getterMethod);
        }
        return ClassUtil.getFieldValue(ownerInstance, field);
    }

    @Override
    public String toString() {
        return fullName;
    }
}
