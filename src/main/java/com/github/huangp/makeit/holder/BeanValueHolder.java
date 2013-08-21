package com.github.huangp.makeit.holder;

import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.reflect.MutableTypeToInstanceMap;
import com.google.common.reflect.TypeToInstanceMap;
import com.google.common.reflect.TypeToken;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class BeanValueHolder
{

   private TypeToInstanceMap<Object> map = new MutableTypeToInstanceMap<Object>();

   public <T> BeanValueHolder putIfNotNull(TypeToken<T> typeToken, T bean)
   {
      if (bean != null)
      {
         map.putInstance(typeToken, bean);
      }
      return this;
   }

   public <T> Optional<T> tryGet(TypeToken<T> typeToken)
   {
      T instance = map.getInstance(typeToken);
      return Optional.fromNullable(instance);
   }

   public <T> BeanValueHolder putIfNotNull(Class<T> type, T bean)
   {
      return putIfNotNull(TypeToken.of(type), bean);
   }

   public <T> Optional<T> tryGet(Class<T> type)
   {
      return tryGet(TypeToken.of(type));
   }

   public void clear()
   {
      map.clear();
   }

   public BeanValueHolder merge(BeanValueHolder other)
   {
      for (Map.Entry<TypeToken<?>, Object> entry : other.map.entrySet())
      {
         TypeToken key = entry.getKey();
         Object value = entry.getValue();
         this.map.putInstance(key, value);
      }
      return this;
   }

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .add("map", map)
            .toString();
   }
}
