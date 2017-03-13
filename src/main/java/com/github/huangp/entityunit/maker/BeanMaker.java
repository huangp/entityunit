package com.github.huangp.entityunit.maker;

import com.github.huangp.entityunit.entity.EntityClass;
import com.github.huangp.entityunit.entity.MakeContext;
import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import javax.persistence.Id;
import javax.persistence.Version;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;

import static com.github.huangp.entityunit.util.HasAnnotationPredicate.has;

/**
 * The core maker that makes java bean.
 * <p>
 * For a given class type, the make process follows certain rules:
 * <pre>
 * 1. Ff it can find public constants, it will use that as value.
 * 2. It will try to use constructor with the most arguments to create a instance.
 * 3. For all its instance fields, it will skip:
 *      - Fields that has the same type as itself.
 *      - Object type field that has default value.
 *      - Field has @Id or @Version annotation.
 *      - Made field value that SkipFieldValueMaker#shouldSkipThisField(java.lang.Object) returns true.
 *        i.e. for primitive type fields, in some cases we may want to provide a value but in other cases the value may
 *        be derived from constructor parameter or populated in PrePersist method.
 * 4. If class is entity class and has access type of field, it will use reflection to set field value.
 *    Otherwise it uses commons bean util to populate properties (which will ignore protected setters).
 * </pre>
 *
 * @see ScalarValueMakerFactory
 * @see SkipFieldValueMaker
 * @author Patrick Huang
 */
@Slf4j
public class BeanMaker<T> implements Maker<T> {
    private final Class<T> type;
    private final MakeContext context;
    private final ScalarValueMakerFactory factory;

    public BeanMaker(Class<T> type, MakeContext context) {
        this.type = type;
        this.context = context;
        factory = new ScalarValueMakerFactory(context);
    }

    @Override
    public T value() {
        T result = null;
        log.debug(">>> bean: {}", type.getName());
        try {
            Constructor<T> constructor = ClassUtil.findMostArgsConstructor(type);
            result = constructBean(constructor);

            // if we can find public static constants defined in the class, we will use that as value
            Optional<T> constants = ClassUtil.tryFindPublicConstants(type, result);
            if (constants.isPresent()) {
                return constants.get();
            }

            // populate all fields
            return setApplicableFields(result);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            log.debug("bean of type {} made: {}", type, result);
            log.debug("<<<");
            context.getBeanValueHolder().putIfNotNull(type, result);
        }
    }

    private T constructBean(Constructor<T> constructor) {
        // this may override some default values provided at field declaration. See HCopyTransOptions
        List<Settable> parameters = ClassUtil.getConstructorParameters(constructor, type);
        List<Object> paramValues = Lists.transform(parameters, new Function<Settable, Object>() {
            @Override
            public Object apply(Settable input) {
                return factory.from(input).value();
            }
        });

        try {
            log.debug("invoke {} constructor with parameters {}", type, parameters);
            constructor.setAccessible(true);
            return constructor.newInstance(paramValues.toArray());
        } catch (Exception e) {
            log.warn("fail calling constructor method: {}. Will fall back to default constructor", constructor);
            log.warn("exception {}", e.getMessage());
            log.debug("exception", e);
            return ClassUtil.invokeNoArgConstructor(type);
        }
    }

    private T setApplicableFields(T result) throws InvocationTargetException, IllegalAccessException {
        Iterable<Settable> elements = EntityClass.from(type).getElements();

        Predicate<Settable> settablePredicate = Predicates.not(
                Predicates.<Settable>or(
                        new SameTypePredicate(result.getClass()),
                        CollectionTypePredicate.PREDICATE,
                        new HasDefaultValuePredicate<T>(result),
                        IdOrVersionPredicate.PREDICATE));

        Iterable<Settable> fieldsToSet = Iterables.filter(elements, settablePredicate);
        for (Settable settable : fieldsToSet) {
            trySetValue(result, settable);
        }
        return result;

    }

    private void trySetValue(T result, Settable settable) {
        log.trace("about to make {}", settable);
        Object fieldValue = factory.from(settable).value();
        // this is ugly. But don't want to change the whole design to fit this feature
        if (fieldValue == null || SkipFieldValueMaker.shouldSkipThisField(fieldValue)) {
            return;
        }
        log.trace("made value {}", fieldValue);
        try {
            if (ClassUtil.isAccessTypeIsField(type)) {
                ClassUtil.setValue(settable, result, fieldValue);
            } else {
                BeanUtils.setProperty(result, settable.getSimpleName(), fieldValue);
            }
        } catch (Exception e) {
            log.warn("can not set property: {}={}", settable, fieldValue);
            log.warn("exception {}", e.getMessage());
            log.debug("exception", e);
        } finally {
            if (log.isDebugEnabled()) {
                String field = Strings.padEnd(settable.getSimpleName(), 20, ' ');
                log.debug("    {} <=     {}", field, settable.valueIn(result));
            }
        }
    }

    @RequiredArgsConstructor
    private static class SameTypePredicate implements Predicate<Settable> {
        private final Class type;

        @Override
        public boolean apply(Settable input) {
            return input.getType().equals(type);
        }
    }

    private static enum CollectionTypePredicate implements Predicate<Settable> {
        PREDICATE;

        @Override
        public boolean apply(Settable input) {
            Type type = input.getType();
            return ClassUtil.isCollection(type) || ClassUtil.isMap(type);
        }
    }

    @RequiredArgsConstructor
    private static class HasDefaultValuePredicate<T> implements Predicate<Settable> {
        private final T object;

        @Override
        public boolean apply(Settable input) {
            try {
                return notPrimitive(input.getType()) && input.valueIn(object) != null;
            } catch (Exception e) {
                log.warn("can not determine field [{}] has default value or not", input);
                log.warn("exception: {}", e.getMessage());
                log.debug("exception", e);
                return false;
            }
        }

        private static boolean notPrimitive(Type settableType) {
            return !ClassUtil.isPrimitive(settableType);
        }

    }

    private static enum IdOrVersionPredicate implements Predicate<Settable> {
        PREDICATE;

        @Override
        public boolean apply(Settable input) {
            return has(Id.class).apply(input) || has(Version.class).apply(input);
        }
    }
}
