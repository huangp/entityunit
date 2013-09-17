package com.github.huangp.makeit.maker;

import com.google.common.base.Objects;

import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor
public class FixedValueMaker<V> implements Maker<V>
{
   public static final FixedValueMaker<Boolean> ALWAYS_TRUE_MAKER = new FixedValueMaker<Boolean>(true);
   public static final FixedValueMaker<String> EMPTY_STRING_MAKER = new FixedValueMaker<String>("");

   private final V fixedValue;

   @Override
   public V value()
   {
      return fixedValue;
   }

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .addValue(fixedValue)
            .toString();
   }

   public static <T> FixedValueMaker<T> fix(T value)
   {
      return new FixedValueMaker<T>(value);
   }
}
