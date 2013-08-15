package org.huangp.makeit.maker;

import org.huangp.makeit.holder.BeanValueHolder;
import org.huangp.makeit.holder.BeanValueHolderImpl;
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
