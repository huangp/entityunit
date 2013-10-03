package com.github.huangp.entityunit.util;

import com.github.huangp.entityunit.entity.EntityClass;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Patrick Huang
 */
@Slf4j
public final class ClassUtil {
    private ClassUtil() {
    }

    /**
     * Return all non-static and non-transient fields.
     *
     * @param type
     *         class to work with
     * @return list of fields
     */
    public static List<Field> getInstanceFields(Class type) {
        List<Field> fields = Lists.newArrayList(type.getDeclaredFields());
        return ImmutableList.copyOf(Iterables.filter(fields, InstanceFieldPredicate.PREDICATE));
    }

    public static List<Field> getAllDeclaredFields(Class clazz) {
        List<Field> fields = Lists.newArrayList(clazz.getDeclaredFields());
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            fields.addAll(Lists.newArrayList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        return ImmutableList.copyOf(Iterables.filter(fields, InstanceFieldPredicate.PREDICATE));
    }

    public static Map<String, PropertyDescriptor> getPropertyDescriptors(Class clazz) {
        try {
            PropertyDescriptor[] propDesc = Introspector.getBeanInfo(clazz, clazz.getSuperclass()).getPropertyDescriptors();
            return Maps.uniqueIndex(Lists.newArrayList(propDesc), new Function<PropertyDescriptor, String>() {
                @Override
                public String apply(PropertyDescriptor input) {
                    return input.getName();
                }
            });
        } catch (IntrospectionException e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> Optional<T> tryFindPublicConstants(final Class<T> type, T instance) throws IllegalAccessException {
        List<Field> fields = Lists.newArrayList(type.getDeclaredFields());
        Optional<Field> found = Iterables.tryFind(fields, new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                int mod = input.getModifiers();
                Class<?> fieldType = input.getType();
                return fieldType.equals(type) && Modifier.isPublic(mod) && Modifier.isStatic(mod);
            }
        });
        if (found.isPresent()) {
            return Optional.of((T) found.get().get(instance));
        }
        return Optional.absent();
    }

    public static <T> Invokable<T, T> findMostArgsConstructor(Class<T> type) {
        List<Constructor<?>> constructors = Lists.newArrayList(type.getDeclaredConstructors());

        // sort by number of parameters in descending order
        Collections.sort(constructors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(Constructor<?> o1, Constructor<?> o2) {
                return o2.getParameterTypes().length - o1.getParameterTypes().length;
            }
        });

        return (Invokable<T, T>) Invokable.from(constructors.get(0));
    }

    public static <T> Invokable<T, T> getNoArgConstructor(Class<T> entityType) {
        try {
            return Invokable.from(entityType.getConstructor());
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }

    public static boolean isAccessTypeIsField(Class clazz) {
        Annotation access = clazz.getAnnotation(Access.class);
        if (access != null) {
            AccessType accessType = ((Access) access).value();
            return accessType == AccessType.FIELD;
        }
        Optional<Field> fieldAnnotatedById = Iterables.tryFind(getAllDeclaredFields(clazz), HasAnnotationPredicate.has(Id.class));
        return fieldAnnotatedById.isPresent();
    }

    public static boolean isCollection(Type type) {
        return Collection.class.isAssignableFrom(TypeToken.of(type).getRawType());
    }

    public static boolean isMap(Type type) {
        return Map.class.isAssignableFrom(TypeToken.of(type).getRawType());
    }

    public static boolean isEntity(Type type) {
        return TypeToken.of(type).getRawType().isAnnotationPresent(Entity.class);
    }

    public static <T> T findEntity(Iterable<Object> entities, Class<T> typeToFind) {
        return (T) Iterables.find(entities, Predicates.instanceOf(typeToFind));
    }

    public static <T> T invokeGetter(Object entity, Method method) {
        try {
            method.setAccessible(true);
            T result = (T) method.invoke(entity);
            return result;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Method getterMethod(Class type, String name) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                if (propertyDescriptor.getName().equals(name)) {
                    return propertyDescriptor.getReadMethod();
                }
            }
        } catch (IntrospectionException e) {
            throw Throwables.propagate(e);
        }
        log.warn("getter method not found: {} - {}", type, name);
        return null;
    }

    public static boolean isUnsaved(Object entity) {
        Settable idSettable = getIdentityField(entity);
        try {
            return idSettable.valueIn(entity) == null;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Settable getIdentityField(Object entity) {
        return Iterables.find(EntityClass.from(entity.getClass()).getElements(), HasAnnotationPredicate.has(Id.class));
    }

    public static void setValue(Settable settable, Object owner, Object value) {
        final String simpleName = settable.getSimpleName();

        try {
            Field field = Iterables.find(getAllDeclaredFields(owner.getClass()), new Predicate<Field>() {
                @Override
                public boolean apply(Field input) {
                    return input.getName().equals(simpleName);
                }
            });
            field.setAccessible(true);
            field.set(owner, value);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    static <T> T getFieldValue(Object ownerInstance, Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(ownerInstance);
        }
        catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    private static enum InstanceFieldPredicate implements Predicate<Field> {
        PREDICATE;

        @Override
        public boolean apply(Field input) {
            int mod = input.getModifiers();
            return !Modifier.isStatic(mod) && !Modifier.isTransient(mod);
        }
    }
}
