package com.github.huangp.makeit.maker;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
class NullMaker<T> implements Maker<T>
{
   @Override
   public T value()
   {
      return null;
   }
}
