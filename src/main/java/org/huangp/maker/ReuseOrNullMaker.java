package org.huangp.maker;

import org.huangp.holder.BeanValueHolder;
import org.huangp.holder.BeanValueHolderImpl;
import com.google.common.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ReuseOrNullMaker implements Maker<Object>
{
   private static BeanValueHolder holder = BeanValueHolderImpl.HOLDER;

   private final Class type;

   @Override
   public Object value()
   {
      return holder.tryGet(TypeToken.of(type)).orNull();
   }
}
