package org.huangp.makeit.entity;

import java.util.Queue;
import javax.persistence.EntityManager;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface EntityPersistService
{
   Queue<Object> getRequiredEntitiesFor(Class entity);

   void persistInOrder(EntityManager entityManager, Queue<Object> queue);

   <T> T createAndPersist(EntityManager entityManager, Class<T> entityType);

   void deleteAll(EntityManager entityManager, Iterable<Class> entities);
}
