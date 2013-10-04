package com.github.huangp.entityunit.util;

import com.github.huangp.entityunit.entity.EntityClass;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
@Slf4j
public class ClassUtilTest {

    @Test
    public void canGetGetterMethodForField() {
        assertThat(ClassUtil.getterMethod(Child.class, "childName").getName(), Matchers.equalTo("getChildName"));
        assertThat(ClassUtil.getterMethod(Child.class, "parentName").getName(), Matchers.equalTo("getParentName"));
        assertThat(ClassUtil.getterMethod(Child.class, "grandName").getName(), Matchers.equalTo("getGrandName"));
    }

    @Data
    class GrandParent {
        private String grandName;
    }

    class Parent extends GrandParent {
        @Getter
        @Setter
        private String parentName;
    }

    class Child extends Parent {
        @Getter
        @Setter
        private String childName;
    }

    @Test
    public void canTestType() {
        List<Settable> elements = Lists.newArrayList(EntityClass.from(Dummy.class).getElements());
        log.debug("{}", elements);

        assertThat(ClassUtil.isArray(elements.get(0).getType()), Matchers.is(true));
        assertThat(ClassUtil.isMap(elements.get(1).getType()), Matchers.is(true));
        assertThat(ClassUtil.isCollection(elements.get(2).getType()), Matchers.is(true));
        assertThat(ClassUtil.isPrimitive(elements.get(3).getType()), Matchers.is(true));
        assertThat(ClassUtil.isCollection(elements.get(4).getType()), Matchers.is(true));
        assertThat(ClassUtil.isArray(elements.get(5).getType()), Matchers.is(true));
    }

    static class Dummy<T> {
        String[] array;
        Map<String, Long> map;
        Set<String> paramSet;
        int primitive;
        List rawList;
        T[] typeArray;

    }
}
