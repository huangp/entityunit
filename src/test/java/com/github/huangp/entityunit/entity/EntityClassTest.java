package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.Settable;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlowTargetReviewComment;

import javax.persistence.OneToOne;
import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang
 */
public class EntityClassTest {

    @Test
    public void canGetFieldAccessTypeEntity() {
        EntityClass entityClass = EntityClass.from(HTextFlowTargetReviewComment.class);

        OneToOne annotation = HProjectIteration.class.getAnnotation(OneToOne.class);
        Iterable<EntityClass> elements = entityClass.getDependingEntityTypes();

        assertThat(elements, Matchers.<EntityClass>iterableWithSize(2));
    }

    @Test
    public void canGetPropertyAccessTypeEntity() {
        EntityClass entityClass = EntityClass.from(HProjectIteration.class);

        Iterable<EntityClass> elements = entityClass.getDependingEntityTypes();

        assertThat(elements, Matchers.<EntityClass>iterableWithSize(2));
    }

    @Test
    public void canGetAllAnnotatedElements() {
        EntityClass entityClass = EntityClass.from(HProjectIteration.class);

        Iterable<Settable> elements = entityClass.getElements();

        assertThat(elements, Matchers.<Settable>iterableWithSize(19));

        assertThat(entityClass.getDependingEntityTypes(), Matchers.<EntityClass>iterableWithSize(2));
        assertThat(entityClass.getContainingEntitiesGetterMethods(), Matchers.<Method>iterableWithSize(3));
    }

    @Test
    public void canGetAllProperties() {
        EntityClass entityClass = EntityClass.from(HProjectIteration.class);

        Iterable<Settable> elements = entityClass.getElements();

        assertThat(elements, Matchers.<Settable>iterableWithSize(19));
    }
}
