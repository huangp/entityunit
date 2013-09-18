package com.github.huangp.entityunit.maker;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.persistence.Id;
import javax.persistence.Version;

import com.github.huangp.entityunit.util.Settable;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.or;

/**
 * @author Patrick Huang
 */
class NumberMaker implements Maker<Number>
{
   private static int counter;

   @Override
   public Number value()
   {
      return next();
   }

   private synchronized static int next()
   {
      return ++counter;
   }

   public static Maker<Number> from(Optional<Settable> optionalAnnotatedElement)
   {
      if (optionalAnnotatedElement.isPresent())
      {
         List<Annotation> annotations = Lists.newArrayList(optionalAnnotatedElement.get().getAnnotations());
         Optional<Annotation> idOrVersion = Iterables.tryFind(annotations,
               or(instanceOf(Id.class), instanceOf(Version.class)));
         if (idOrVersion.isPresent())
         {
            return new NullMaker<Number>();
         }
      }
      return new NumberMaker();
   }
}
