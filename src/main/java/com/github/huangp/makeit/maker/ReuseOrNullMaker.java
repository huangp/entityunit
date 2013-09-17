package com.github.huangp.makeit.maker;

import com.github.huangp.makeit.holder.BeanValueHolder;
import com.google.common.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ReuseOrNullMaker implements Maker<Object>
{
   private final BeanValueHolder holder;

   private final Class type;

   @Override
   public Object value()
   {
      return holder.tryGet(TypeToken.of(type)).orNull();
   }
}
