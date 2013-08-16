package org.huangp.makeit.util;

import java.lang.reflect.Field;

import com.google.common.base.Function;

import lombok.RequiredArgsConstructor;

/**
* @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
*/
@RequiredArgsConstructor
public class FieldToSettableFunction implements Function<Field, Settable>
{
   private final Class ownerType;

   @Override
   public Settable apply(Field input)
   {
      return SettableField.from(ownerType, input);
   }
}
