package com.github.huangp.entityunit.util;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Getter
public class Parameter implements AnnotatedElement {
    private final Type type;
    private final int order;
    private final List<Annotation> annotations;

    public Parameter(Type type, int order, Annotation[] annotations) {
        this.type = type;
        this.order = order;
        this.annotations = ImmutableList.copyOf(annotations);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotationClass.isInstance(annotation)) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations.toArray(new Annotation[annotations.size()]);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return "arg" + order;
    }
}
