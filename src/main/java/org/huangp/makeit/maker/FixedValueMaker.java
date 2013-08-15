package org.huangp.makeit.maker;

import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@RequiredArgsConstructor
public class FixedValueMaker<V> implements Maker<V>
{
   private final V fixedValue;

   @Override
   public V value()
   {
      return fixedValue;
   }
}
