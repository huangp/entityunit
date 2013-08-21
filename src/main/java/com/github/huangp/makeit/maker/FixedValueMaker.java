package com.github.huangp.makeit.maker;

import com.google.common.base.Objects;

import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@RequiredArgsConstructor
public class FixedValueMaker<V> implements Maker<V>
{
   public static final FixedValueMaker<Boolean> ALWAYS_TRUE_MAKER = new FixedValueMaker<Boolean>(true);

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
}
