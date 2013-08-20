package org.huangp.makeit.maker;

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
}
