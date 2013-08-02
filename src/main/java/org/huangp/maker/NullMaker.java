package org.huangp.maker;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
class NullMaker implements Maker<Object>
{
   @Override
   public Object value()
   {
      return null;
   }
}
