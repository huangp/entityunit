package org.huangp.makeit.holder;

import com.google.common.base.Optional;
import com.google.common.reflect.MutableTypeToInstanceMap;
import com.google.common.reflect.TypeToInstanceMap;
import com.google.common.reflect.TypeToken;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class BeanValueHolderImpl implements BeanValueHolder
{

   private TypeToInstanceMap<Object> map = new MutableTypeToInstanceMap<Object>();

   @Override
   public <T> BeanValueHolder putIfNotNull(TypeToken<T> typeToken, T bean)
   {
      if (bean != null)
      {
         map.putInstance(typeToken, bean);
      }
      return this;
   }

   @Override
   public <T> Optional<T> tryGet(TypeToken<T> typeToken)
   {
      T instance = map.getInstance(typeToken);
      return Optional.fromNullable(instance);
   }

   @Override
   public <T> BeanValueHolder putIfNotNull(Class<T> type, T bean)
   {
      return putIfNotNull(TypeToken.of(type), bean);
   }

   @Override
   public <T> Optional<T> tryGet(Class<T> type)
   {
      return tryGet(TypeToken.of(type));
   }

   @Override
   public void clear()
   {
      map.clear();
   }
}
