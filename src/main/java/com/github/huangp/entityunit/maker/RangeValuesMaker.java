package com.github.huangp.entityunit.maker;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * @author Patrick Huang
 */
public class RangeValuesMaker<T> implements Maker<T>
{
   private final Iterator<T> iterator;

   private RangeValuesMaker(Iterator<T> iterator)
   {
      this.iterator = iterator;
   }

   @Override
   public T value()
   {
      return iterator.next();
   }

   public static <T> Maker<T> cycle(T first, T... rest)
   {
      ImmutableList<T> values = ImmutableList.<T>builder().add(first).add(rest).build();
      return new RangeValuesMaker<T>(Iterables.cycle(values).iterator());
   }

   public static <T> Maker<T> errorOnEnd(T first, T... rest)
   {
      ImmutableList<T> values = ImmutableList.<T>builder().add(first).add(rest).build();
      return new RangeValuesMaker<T>(values.iterator());
   }
}
