package com.github.huangp.makeit.util;

import java.lang.reflect.Method;

import org.hamcrest.Matchers;
import org.junit.Test;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import static org.hamcrest.MatcherAssert.*;

/**
 * @author Patrick Huang
 */
public class ClassUtilTest
{

   @Test
   public void canGetGetterMethodForField()
   {
      assertThat(ClassUtil.getterMethod(Child.class, "childName").getName(), Matchers.equalTo("getChildName"));
      assertThat(ClassUtil.getterMethod(Child.class, "parentName").getName(), Matchers.equalTo("getParentName"));
      assertThat(ClassUtil.getterMethod(Child.class, "grandName").getName(), Matchers.equalTo("getGrandName"));
   }

   @Data
   class GrandParent
   {
      private String grandName;
   }

   class Parent extends GrandParent
   {
      @Getter @Setter
      private String parentName;
   }

   class Child extends Parent
   {
      @Getter @Setter
      private String childName;
   }
}
