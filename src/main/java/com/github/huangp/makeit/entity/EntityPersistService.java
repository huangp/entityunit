package com.github.huangp.makeit.entity;

import java.util.Queue;
import javax.persistence.EntityManager;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface EntityPersistService
{
   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType);

   <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback);

   void deleteAll(EntityManager entityManager, Iterable<Class> entities);

   MakeContext getCurrentContext();

   public interface Callback
   {
      Queue<Object> beforePersist(Queue<Object> toBePersisted);
   }
}
