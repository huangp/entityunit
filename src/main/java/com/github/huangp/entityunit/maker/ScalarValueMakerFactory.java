package com.github.huangp.entityunit.maker;

import com.github.huangp.entityunit.entity.MakeContext;
import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.util.ClassUtil;
import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * A Maker factory for field/property or constructor parameter value making.
 * <p/>
 * If MakeContext.getPreferredValueMakers().getMaker(com.github.huangp.entityunit.util.Settable) returns a matched maker,
 * it will take precedence.
 * <p/>
 * Otherwise base on the settable type, it will create different makers.
 * <pre>
 * For primitive type, a maker that uses primitive default values.
 * For String type, a maker that generates random string but respects JSR303 Size annotation and email (if applicable).
 * For Date type, a maker that returns current date.
 * For Number type and sub types, a maker that generates random integer.
 * For array, collection and map type, a maker always return null.
 * For enum type, a maker returns the first enum constant.
 * For Entity type, it will try to reuse from BeanValueHolder or null.
 * For any other type, assuming it's a bean and return a BeanMaker.
 * </pre>
 *
 * @author Patrick Huang
 * @see PreferredValueMakersRegistry
 * @see BeanValueHolder
 * @see BeanMaker
 * @see MakeContext
 */
@Slf4j
@RequiredArgsConstructor
public class ScalarValueMakerFactory {
    private final MakeContext context;

    /**
     * produce a maker for given settable
     *
     * @param settable
     *         settable
     * @return maker
     */
    public Maker from(Settable settable) {
        Optional<Maker<?>> makerOptional = context.getPreferredValueMakers().getMaker(settable);
        if (makerOptional.isPresent()) {
            return makerOptional.get();
        }

        Type type = settable.getType();
        Class<?> rawType = ClassUtil.getRawType(type);

        if (ClassUtil.isPrimitive(type)) {
            return new PrimitiveMaker(type);
        }
        if (type == String.class) {
            return StringMaker.from(settable);
        }
        if (type == Date.class) {
            return new DateMaker();
        }
        if (Number.class.isAssignableFrom(rawType)) {
            return NumberMaker.from(settable);
        }
        if (ClassUtil.isArray(type)) {
            log.trace("array type: {}", rawType.getComponentType());
            return new NullMaker();
        }
        if (ClassUtil.isEnum(type)) {
            log.trace("enum type: {}", type);
            return new EnumMaker(rawType.getEnumConstants());
        }
        if (ClassUtil.isCollection(type)) {
            log.trace("collection: {}", type);
            return new NullMaker();
        }
        if (ClassUtil.isMap(type)) {
            log.trace("map: {}", type);
            return new NullMaker<Object>();
        }
        if (ClassUtil.isEntity(type)) {
            log.trace("{} is entity type", type);
            // we don't want to make unnecessary entities
            // @see EntityMakerBuilder
            return new ReuseOrNullMaker(context.getBeanValueHolder(), rawType);
        }
        log.debug("guessing this is a bean {}", type);
        return new BeanMaker(rawType, context);
    }

}
