package org.huangp.holder;

import java.io.Serializable;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;

public interface BeanValueHolder
{
   <T> BeanValueHolder putIfNotNull(TypeToken<T> typeToken, T bean);

   <T> Optional<T> tryGet(TypeToken<T> typeToken);

   <T> BeanValueHolder putIfNotNull(Class<T> type, T bean);

   <T> Optional<T> tryGet(Class<T> type);

   void clear();
}
