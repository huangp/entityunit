package com.github.huangp.makeit.entity;

import javax.persistence.EntityManager;

import com.github.huangp.makeit.holder.BeanValueHolder;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface EntityPersistService
{
   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType);

   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback);

   void deleteAll(EntityManager entityManager, Iterable<Class> entityClasses);

   void deleteAllExcept(EntityManager entityManager, Iterable<Class> entityClasses, Object... excludedEntities);

   // TODO do we need this anymore? TakeCopyCallback should handle it
   BeanValueHolder exportCopyOfBeans();

   public interface Callback
   {
      Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted);
   }
}
