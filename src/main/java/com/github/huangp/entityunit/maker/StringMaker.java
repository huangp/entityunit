package com.github.huangp.entityunit.maker;

import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.annotation.Annotation;

/**
 * @author Patrick Huang
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class StringMaker implements Maker<String> {
    public static final int DEFAULT_MAX = 10;
    private final boolean isEmail;
    private final int min;
    private final int max;

    public static StringMaker from(Settable settable) {

        boolean isEmail = false;
        int min = 0;
        int max = DEFAULT_MAX;
        for (Annotation annotation : settable.getAnnotations()) {
            if (looksLikeEmail(settable, annotation)) {
                isEmail = true;
            }
            if (annotation instanceof Size) {
                Size size = (Size) annotation;
                min = size.min();
                if (size.max() != Integer.MAX_VALUE) {
                    max = size.max();
                } else {
                    max = Math.max(min, DEFAULT_MAX);
                }
            }
            if (annotation instanceof Max) {
                Max maxAnnotation = (Max) annotation;
                max = Long.valueOf(maxAnnotation.value()).intValue();
            }
            if (annotation instanceof Min) {
                Min minAnnotation = (Min) annotation;
                // This may lose some precision
                min = Long.valueOf(minAnnotation.value()).intValue();
            }
            if (annotation instanceof Pattern) {
                log.warn("can not auto generate string matches pattern constraint for {}", settable.fullyQualifiedName());
            }
        }
        return new StringMaker(isEmail, min, max);
    }

    private static boolean looksLikeEmail(Settable settable, Annotation annotation) {
        return annotation.annotationType().getName().endsWith("Email") || settable.getSimpleName().equals("email");
    }

    @Override
    public String value() {
        if (isEmail) {
            return RandomStringUtils.randomAlphabetic(5) + "@nowhere.org";
        }
        int length = Math.min(DEFAULT_MAX, max);
        return RandomStringUtils.randomAlphabetic(Math.max(length, min));
    }
}
