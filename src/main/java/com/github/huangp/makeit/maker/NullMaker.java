package com.github.huangp.makeit.maker;

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
