package org.huangp.makeit.maker;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
class NumberMaker implements Maker<Number>
{
   private static int counter;

   @Override
   public Number value()
   {
      return next();
   }

   private synchronized static int next()
   {
      return ++counter;
   }
}
