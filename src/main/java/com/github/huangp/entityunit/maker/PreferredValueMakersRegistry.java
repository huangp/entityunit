package com.github.huangp.entityunit.maker;

import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.HashMap;
import java.util.Map;

/**
 * As the name suggested if random value is not desirable for some fields, you can register a custom maker for them.
 *
 * @author Patrick Huang
 * @see Settable
 * @see #getMaker(com.github.huangp.entityunit.util.Settable)
 * @see ScalarValueMakerFactory
 */
public class PreferredValueMakersRegistry {

    private Map<Matcher<?>, Maker<?>> makers = new HashMap<Matcher<?>, Maker<?>>();

    /**
     * Add a maker with custom matcher.
     *
     * @param settableMatcher
     *         a matcher to match on com.github.huangp.entityunit.util.Settable#fullyQualifiedName()
     * @param maker
     *         custom maker
     * @return this
     */
    public PreferredValueMakersRegistry add(Matcher<?> settableMatcher, Maker<?> maker) {
        Preconditions.checkNotNull(settableMatcher);
        Preconditions.checkNotNull(maker);
        makers.put(settableMatcher, maker);
        return this;
    }

    /**
     * Merge custom makers from another registry.
     *
     * @param otherRegistry
     *         other registry
     * @return this
     */
    public PreferredValueMakersRegistry merge(PreferredValueMakersRegistry otherRegistry) {
        makers.putAll(otherRegistry.makers);
        return this;
    }

    /**
     * Add a field/property maker to a class type.
     * i.e. with:
     * <pre>
     * {@code
     *
     * Class Person {
     *   String name
     * }
     * }
     * </pre>
     * You could define a custom maker for name field as (Person.class, "name", myNameMaker)
     *
     * @param ownerType
     *         the class that owns the field.
     * @param propertyName
     *         field or property name
     * @param maker
     *         custom maker
     * @return this
     */
    public PreferredValueMakersRegistry addFieldOrPropertyMaker(Class ownerType, String propertyName, Maker<?> maker) {
        Preconditions.checkNotNull(ownerType);
        Preconditions.checkNotNull(propertyName);
        makers.put(Matchers.equalTo(String.format(Settable.FULL_NAME_FORMAT, ownerType.getName(), propertyName)), maker);
        return this;
    }

    /**
     * Add a constructor parameter maker to a class type.
     * i.e. with:
     * <pre>
     * {@code
     *
     * Class Person {
     *   Person(String name, int age)
     * }
     * }
     * </pre>
     * You could define a custom maker for name as (Person.class, 0, myNameMaker)
     *
     * @param ownerType
     *         the class that owns the field.
     * @param argIndex
     *         constructor parameter index (0 based)
     * @param maker
     *         custom maker
     * @return this
     */
    public PreferredValueMakersRegistry addConstructorParameterMaker(Class ownerType, int argIndex, Maker<?> maker) {
        Preconditions.checkNotNull(ownerType);
        Preconditions.checkArgument(argIndex >= 0);
        makers.put(Matchers.equalTo(String.format(Settable.FULL_NAME_FORMAT, ownerType.getName(), "arg" + argIndex)), maker);
        return this;
    }

    /**
     * Try to get a registered maker for a settable.
     * It will use the key (Matcher) to match com.github.huangp.entityunit.util.Settable#fullyQualifiedName(),
     * if there is a match found, it will return that.
     *
     * @param settable
     *         settable
     * @return Optional maker
     */
    public Optional<Maker<?>> getMaker(Settable settable) {
        for (Matcher<?> matcher : makers.keySet()) {
            if (matcher.matches(settable.fullyQualifiedName())) {
                return Optional.<Maker<?>>of(makers.get(matcher));
            }
        }
        return Optional.absent();
    }

    /**
     * Clear the internal registry map.
     *
     * @return this
     */
    public PreferredValueMakersRegistry clear() {
        makers.clear();
        return this;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("makers", makers)
                .toString();
    }
}
