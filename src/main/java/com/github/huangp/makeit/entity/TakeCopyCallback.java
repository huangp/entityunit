package com.github.huangp.makeit.entity;

import java.util.List;
import javax.persistence.EntityManager;

import com.github.huangp.makeit.util.ClassUtil;
import com.google.common.collect.ImmutableList;

import lombok.Getter;

/**
* @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
*/
public class TakeCopyCallback implements EntityPersistService.Callback
{
   @Getter
   private List<Object> copy;

   @Override
   public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted)
   {
      copy = ImmutableList.copyOf(toBePersisted);
      return toBePersisted;
   }

   public <T> T getByIndex(int index)
   {
      return (T) copy.get(index);
   }

   public <T> T getByType(Class<T> type)
   {
      return ClassUtil.findEntity(copy, type);
   }

}
