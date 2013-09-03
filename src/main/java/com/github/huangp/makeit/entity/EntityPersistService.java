package com.github.huangp.makeit.entity;

import java.util.Queue;
import javax.persistence.EntityManager;

import com.github.huangp.makeit.holder.BeanValueHolder;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface EntityPersistService
{
   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType);

   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback);

   void wireManyToMany(EntityManager entityManager, Object a, Object b);

   void deleteAll(EntityManager entityManager, Iterable<Class> entities);

   BeanValueHolder exportImmutableCopyOfBeans();

   public interface Callback
   {
      Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted);
   }
}
