package com.github.huangp.makeit.entity;

import javax.persistence.EntityManager;

import com.google.common.collect.ImmutableList;

import static com.github.huangp.makeit.entity.EntityPersister.*;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class Callbacks
{
   public static TakeCopyCallback takeCopy()
   {
      return new TakeCopyCallback();
   }

   public static WireManyToManyCallback wireManyToMany(Class typeToFind, Object objectToWire)
   {
      return new WireManyToManyCallback(typeToFind, objectToWire);
   }

   public static Callback chain(Callback one, Callback... others)
   {
      return new ChainedCallback(one, others);
   }


   private static class ChainedCallback implements Callback
   {
      private final ImmutableList<EntityPersister.Callback> callbacks;

      public ChainedCallback(Callback one, Callback... rest)
      {
         callbacks = ImmutableList.<Callback>builder().add(one).add(rest).build();
      }

      @Override
      public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted)
      {
         Iterable<Object> toReturn = toBePersisted;
         for (Callback callback : callbacks)
         {
            toReturn = callback.beforePersist(entityManager, toBePersisted);
         }
         return toReturn;
      }
   }
}
