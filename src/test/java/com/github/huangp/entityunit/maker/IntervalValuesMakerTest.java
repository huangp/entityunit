package com.github.huangp.entityunit.maker;

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;

/**
 * @author Patrick Huang
 */
public class IntervalValuesMakerTest
{
   @Test
   public void canGetIntervalInteger() {
      Maker<Integer> maker = IntervalValuesMaker.startFrom(1, 2);

      assertThat(maker.value(), Matchers.equalTo(1));
      assertThat(maker.value(), Matchers.equalTo(3));
      assertThat(maker.value(), Matchers.equalTo(5));
   }

   @Test
   public void canGetIntervalLong() {
      Maker<Long> maker = IntervalValuesMaker.startFrom(1L, 2);

      assertThat(maker.value(), Matchers.equalTo(1L));
      assertThat(maker.value(), Matchers.equalTo(3L));
      assertThat(maker.value(), Matchers.equalTo(5L));
   }

   @Test
   public void canGetIntervalDate() {
      Date start = new Date();
      Maker<Date> maker = IntervalValuesMaker.startFrom(start, -1000);

      assertThat(maker.value().getTime(), Matchers.equalTo(start.getTime()));
      assertThat(maker.value().getTime(), Matchers.equalTo(start.getTime() - 1000));
      assertThat(maker.value().getTime(), Matchers.equalTo(start.getTime() - 2000));
   }

   @Test
   public void canGetIntervalString() {
      Maker<String> maker = IntervalValuesMaker.startFrom("hello ", 1000);

      assertThat(maker.value(), Matchers.equalTo("hello 1000"));
      assertThat(maker.value(), Matchers.equalTo("hello 2000"));
      assertThat(maker.value(), Matchers.equalTo("hello 3000"));
   }
}
