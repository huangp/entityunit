package com.github.huangp.entityunit.maker;

import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.persistence.Id;
import javax.persistence.Version;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.or;

/**
 * @author Patrick Huang
 */
class NumberMaker implements Maker<Number> {
    private static AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Number value() {
        return next();
    }

    private static int next() {
        return counter.incrementAndGet();
    }

    public static Maker<Number> from(Settable settable) {
        List<Annotation> annotations = Lists.newArrayList(settable.getAnnotations());
        Optional<Annotation> idOrVersion = Iterables.tryFind(annotations,
                or(instanceOf(Id.class), instanceOf(Version.class)));
        if (idOrVersion.isPresent()) {
            return new NullMaker<Number>();
        }
        return new NumberMaker();
    }
}
