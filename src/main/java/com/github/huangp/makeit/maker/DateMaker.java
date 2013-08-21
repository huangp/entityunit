package com.github.huangp.makeit.maker;

import java.util.Date;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
class DateMaker implements Maker<Date>
{
   @Override
   public Date value()
   {
      return new Date();
   }
}
