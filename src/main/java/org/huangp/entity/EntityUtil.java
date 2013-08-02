package org.huangp.entity;

import java.util.List;
import java.util.Queue;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class EntityUtil
{
   private static final String NEW_LINE = "\n";
   private static final String THEN = "    ==> ";

   public static String prettyPrint(Queue<Object> entityQueue)
   {
      List<Object> entities = ImmutableList.copyOf(entityQueue);
      StringBuilder builder = new StringBuilder();
      builder.append(NEW_LINE);
      for (Object entity : entities)
      {
         builder.append(THEN).append(entity);
         builder.append(NEW_LINE);
      }
      return builder.toString();
   }

   public static <T> Optional<T> findEntity(Queue<Object> entityQueue, Class<T> typeToFind)
   {
      List<Object> entities = ImmutableList.copyOf(entityQueue);
      return (Optional<T>) Iterables.tryFind(entities, Predicates.instanceOf(typeToFind));
   }
}
