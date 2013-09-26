package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.Settable;
import com.github.huangp.entityunit.util.SettableField;
import com.github.huangp.entityunit.util.SettableProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.github.huangp.entityunit.util.ClassUtil.getInstanceFields;
import static com.github.huangp.entityunit.util.ClassUtil.getReadablePropertyDescriptors;
import static com.github.huangp.entityunit.util.ClassUtil.isAccessTypeIsField;
import static com.github.huangp.entityunit.util.HasAnnotationPredicate.has;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Adaptor of entity class.
 * <p/>
 * It scans the class hierarchy and figure out what elements are of interest when making an entity bean.
 * <p/>
 * It supports both access type: field and property.
 * <p/>
 * Example:
 * <pre>
 * <code>
 *
 * {@literal@}MappedSuperclass
 * class Identifier {
 *     {@literal@}Id
 *     Long id;
 * }
 *
 * {@literal@}Entity
 * class LineItem extends Identifier {
 *     {@literal@}ManyToOne
 *     Category category;
 *
 *     {@literal@}OneToOne
 *     Person owner;
 *
 *     String name;
 *
 *     {@literal@}OneToMany
 *     List<Item> items;
 * }
 * </code>
 * // The resulting object is cached so calling it again with same arguments it won't do the reflection scan again.
 * EntityClass entityClass = EntityClass.from(LineItem.class, ScanOption.IncludeOneToOne);
 * // entityClass.getDependingEntityTypes() will return Category and Person (since we include optional one to one mapping)
 * // entityClass.getElements() will return all elements including id, name, owner, category
 * // entityClass.getContainingEntitiesGetterMethods will return getItems() method
 * // entityClass.getManyToManyMethods will return empty
 * </pre>
 * <p/>
 *
 * @author Patrick Huang
 */
@ToString(of = "type")
@EqualsAndHashCode(of = {"type", "scanOption"})
public class EntityClass {
    private static final Cache<CacheKey, EntityClass> CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();

    private Predicate<AnnotatedElement> oneToOnePredicate;
    @Getter
    private final Class type;
    private final ScanOption scanOption;
    @Getter
    private final Iterable<Settable> elements;

    @Getter
    @Setter
    private boolean requireNewInstance;

    private transient Iterable<EntityClass> requiredEntityTypes;
    private transient Iterable<Method> associationGetters;
    private transient Iterable<Method> manyToManyGetters;

    private EntityClass(Class type, Iterable<Settable> elements, ScanOption scanOption) {
        this.type = type;
        this.scanOption = scanOption;
        List<Settable> settables = newArrayList(elements);
        Collections.sort(settables, NameComparator.COMPARATOR);
        this.elements = ImmutableList.copyOf(settables);
        if (scanOption == ScanOption.IgnoreOptionalOneToOne) {
            oneToOnePredicate = Predicates.and(has(OneToOne.class), RequiredOneToOnePredicate.PREDICATE);
        } else {
            oneToOnePredicate = Predicates.and(has(OneToOne.class), has(JoinColumn.class));
        }
    }

    public static EntityClass from(final Class clazz) {
        return from(clazz, ScanOption.IgnoreOptionalOneToOne);
    }

    /**
     * Factory method.
     *
     * @param clazz
     *         the class to wrap
     * @param scanOption
     *         whether consider optional OneToOne as required
     * @return a wrapper for the entity class
     */
    public static EntityClass from(final Class clazz, final ScanOption scanOption) {
        try {
            return CACHE.get(CacheKey.of(clazz, scanOption), new Callable<EntityClass>() {
                @Override
                public EntityClass call() throws Exception {
                    return createEntityClass(clazz, scanOption);
                }
            });
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    private static EntityClass createEntityClass(Class rootClass, ScanOption scanOption) {
        List<Settable> settables = Lists.newArrayList(getSettables(rootClass, rootClass));
        Class<?> superClass = rootClass.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            settables.addAll(getSettables(rootClass, superClass));
            superClass = superClass.getSuperclass();
        }
        return new EntityClass(rootClass, settables, scanOption);
    }

    private static List<Settable> getSettables(Class rootClass, Class targetClass) {
        if (isAccessTypeIsField(targetClass)) {
            // field based annotation
            List<Field> fields = getInstanceFields(targetClass);
            return Lists.transform(fields, new FieldToSettableFunction(rootClass));
        } else {
            // property based annotation
            Iterable<PropertyDescriptor> descriptors = getReadablePropertyDescriptors(targetClass);
            return Lists.newArrayList(Iterables.transform(descriptors, new PropertyToSettableFunction(rootClass)));
        }
    }

    /**
     * @return Other entity classes that is required to persist this entity
     */
    public Iterable<EntityClass> getDependingEntityTypes() {
        if (requiredEntityTypes == null) {
            Iterable<EntityClass> manyToOne = transform(filter(elements, has(ManyToOne.class)), new TypeFunction(scanOption, false));
            Iterable<EntityClass> oneToOne = transform(filter(elements, oneToOnePredicate), new TypeFunction(scanOption, true));
            requiredEntityTypes = Iterables.concat(manyToOne, oneToOne);
        }
        return requiredEntityTypes;
    }

    /**
     * @return getter methods that will return associations (OneToMany collections)
     */
    public Iterable<Method> getContainingEntitiesGetterMethods() {
        if (associationGetters == null) {
            Iterable<Settable> oneToMany = filter(elements, has(OneToMany.class));
            associationGetters = transform(oneToMany, SettableGetterMethodFunction.FUNCTION);
        }
        return associationGetters;
    }

    /**
     * @return manyToMany mapping getter methods
     */
    public Iterable<Method> getManyToManyMethods() {
        if (manyToManyGetters == null) {
            Iterable<Settable> manyToMany = filter(getElements(), Predicates.and(has(ManyToMany.class), has(JoinTable.class)));
            manyToManyGetters = transform(manyToMany, SettableGetterMethodFunction.FUNCTION);
        }
        return manyToManyGetters;
    }

    private static enum NameComparator implements Comparator<Settable> {
        COMPARATOR;

        @Override
        public int compare(Settable o1, Settable o2) {
            return o1.getSimpleName().compareTo(o2.getSimpleName());
        }
    }

    @RequiredArgsConstructor
    private static class TypeFunction implements Function<Settable, EntityClass> {
        private final ScanOption scanOption;
        private final boolean requireNewInstance;

        @Override
        public EntityClass apply(Settable input) {
            EntityClass entityClass = EntityClass.from(TypeToken.of(input.getType()).getRawType(), scanOption);
            entityClass.setRequireNewInstance(requireNewInstance);
            return entityClass;
        }
    }

    private static enum SettableGetterMethodFunction implements Function<Settable, Method> {
        FUNCTION;

        @Override
        public Method apply(Settable input) {
            return input.getterMethod();
        }
    }

    private static enum RequiredOneToOnePredicate implements Predicate<AnnotatedElement> {
        PREDICATE;

        @Override
        public boolean apply(AnnotatedElement input) {
            OneToOne oneToOne = input.getAnnotation(OneToOne.class);
            return oneToOne != null && !oneToOne.optional();
        }
    }

    @RequiredArgsConstructor
    private static class PropertyToSettableFunction implements Function<PropertyDescriptor, Settable> {
        private final Class ownerType;

        @Override
        public Settable apply(PropertyDescriptor input) {
            return SettableProperty.from(ownerType, input);
        }
    }

    @RequiredArgsConstructor
    private static class FieldToSettableFunction implements Function<Field, Settable> {
        private final Class ownerType;

        @Override
        public Settable apply(Field input) {
            return SettableField.from(ownerType, input);
        }
    }
}
