package com.github.huangp.entityunit.maker;

/**
 * @author Patrick Huang
 */
class NullMaker<T> implements Maker<T>
{
   @Override
   public T value()
   {
      return null;
   }
}
