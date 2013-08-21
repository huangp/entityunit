package com.github.huangp.makeit.entity;

import java.lang.reflect.Method;

import org.hamcrest.Matchers;
import com.github.huangp.makeit.util.Settable;
import org.junit.Test;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlowTargetReviewComment;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class EntityClassTest
{

   @Test
   public void canGetFieldAccessTypeEntity()
   {
      EntityClass entityClass = EntityClass.from(HTextFlowTargetReviewComment.class);

      Iterable<Class<?>> elements = entityClass.getDependingEntityTypes();

      assertThat(elements, Matchers.<Class<?>> iterableWithSize(2));
   }

   @Test
   public void canGetPropertyAccessTypeEntity()
   {
      EntityClass entityClass = EntityClass.from(HProjectIteration.class);

      Iterable<Class<?>> elements = entityClass.getDependingEntityTypes();

      assertThat(elements, Matchers.<Class<?>> iterableWithSize(2));
   }

   @Test
   public void canGetAllAnnotatedElements()
   {
      EntityClass entityClass = EntityClass.from(HProjectIteration.class);

      Iterable<Settable> elements = entityClass.getElements();

      assertThat(elements, Matchers.<Settable> iterableWithSize(19));

      assertThat(entityClass.getDependingEntityTypes(), Matchers.<Class<?>> iterableWithSize(2));
      assertThat(entityClass.getContainingEntitiesGetterMethods(), Matchers.<Method> iterableWithSize(3));
   }

   @Test
   public void canGetAllProperties()
   {
      EntityClass entityClass = EntityClass.from(HProjectIteration.class);

      Iterable<Settable> elements = entityClass.getElements();

      assertThat(elements, Matchers.<Settable> iterableWithSize(19));
   }
}
