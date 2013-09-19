package com.github.huangp.entityunit.entity;

import javax.persistence.EntityManager;

import com.github.huangp.entityunit.holder.BeanValueHolder;

/**
 * @author Patrick Huang
 */
public interface EntityMaker
{
   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType);

   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback);

   // TODO do we need this anymore? TakeCopyCallback should handle it
   BeanValueHolder exportCopyOfBeans();

   public interface Callback
   {
      Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted);
   }
}
