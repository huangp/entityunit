package com.github.huangp.entityunit.entity;

import javax.persistence.EntityManager;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class AbstractNoOpCallback implements EntityMaker.Callback
{
   public static final AbstractNoOpCallback NO_OP_CALLBACK = new AbstractNoOpCallback();

   @Override
   public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted)
   {
      return toBePersisted;
   }

   @Override
   public Iterable<Object> afterPersist(EntityManager entityManager, Iterable<Object> persisted)
   {
      return persisted;
   }
}
