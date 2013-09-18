package com.github.huangp.entityunit.maker;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import com.github.huangp.entityunit.util.Settable;
import com.github.huangp.entityunit.util.SettableProperty;
import org.junit.Test;
import org.zanata.model.HAccount;
import org.zanata.model.HPerson;

import com.google.common.base.Optional;

/**
 * @author Patrick Huang
 */
public class StringMakerTest
{

   @Test
   public void canMakeRandomString()
   {
      StringMaker maker = StringMaker.from(Optional.<Settable> absent());

      String value = maker.value();

      assertThat(value, Matchers.notNullValue());
      assertThat(value.length(), Matchers.equalTo(StringMaker.DEFAULT_MAX));
   }

   @Test
   public void canMakeStringWithEmailConstraint() throws NoSuchMethodException
   {

      StringMaker maker = StringMaker.from(Optional.of(SettableProperty.from(HPerson.class, HPerson.class.getMethod("getEmail"))));

      String value = maker.value();

      assertThat(value, Matchers.endsWith("@nowhere.org"));
   }

   @Test
   public void canMakeStringWithSizeLimit() throws NoSuchMethodException
   {
      StringMaker maker = StringMaker.from(Optional.of(SettableProperty.from(HAccount.class, HAccount.class.getMethod("getApiKey"))));

      String value = maker.value();

      assertThat(value.length(), Matchers.equalTo(32));
   }
}
