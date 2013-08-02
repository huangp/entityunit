package org.huangp.scanner;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.persistence.Entity;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class EntityClassScanner implements ClassScanner
{
   @Override
   public Iterable<EntityClass> scan(Class clazz)
   {
      List<EntityClass> current = Lists.newArrayList();
      recursiveScan(clazz, current);
      return ImmutableList.copyOf(Lists.reverse(current));
   }

   private static void recursiveScan(final Class clazz, List<EntityClass> current)
   {
      String classUnderScan = clazz.getName();
      log.debug("scanning class: {}", classUnderScan);

      Annotation entityAnnotation = clazz.getAnnotation(Entity.class);
      Preconditions.checkState(entityAnnotation != null, "This scans only entity class");

      EntityClass startNode = getOrCreateNode(current, clazz);
      if (startNode.isScanned())
      {
         log.trace("{} has been scanned", startNode);
         return;
      }
      Iterable<Class<?>> dependingTypes = startNode.getDependingEntityTypes();

      for (Class<?> dependingType : dependingTypes)
      {
         EntityClass dependingNode = getOrCreateNode(current, dependingType);

         if (!clazz.equals(dependingNode.getType()))
         {
            current.add(dependingNode);
         }
         if (shouldScanDependingNode(dependingNode, clazz))
         {
            recursiveScan(dependingNode.getType(), current);
         }
      }
      startNode.markScanned();
   }

   private static EntityClass getOrCreateNode(List<EntityClass> current, final Class<?> entityType)
   {
      Optional<EntityClass> dependingOptional = Iterables.tryFind(current, new Predicate<EntityClass>()
      {
         @Override
         public boolean apply(EntityClass input)
         {
            return input.getType() == entityType;
         }
      });
      return dependingOptional.isPresent() ? dependingOptional.get() : EntityClassImpl.from(entityType);
   }

   private static boolean shouldScanDependingNode(EntityClass dependingNode, Class clazz)
   {
      return !dependingNode.isScanned() && !dependingNode.getType().equals(clazz);
   }


}
