package org.huangp.maker;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
class EnumMaker implements Maker<Object>
{
   private final List<Object> enums;

   public EnumMaker(Object[] enumConstants)
   {
      enums = ImmutableList.copyOf(enumConstants);
   }

   @Override
   public Object value()
   {
      return enums.get(0);
   }
}
