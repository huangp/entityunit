package com.github.huangp.makeit.maker;

import com.google.common.base.Defaults;

/**
 * @author Patrick Huang
 */
class PrimitiveMaker implements Maker
{
   private final Class<?> type;

   public PrimitiveMaker(Class<?> type)
   {
      this.type = type;
   }

   @Override
   public Object value()
   {
      return Defaults.defaultValue(type);
   }
}
