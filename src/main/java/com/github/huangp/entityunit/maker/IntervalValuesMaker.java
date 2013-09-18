package com.github.huangp.entityunit.maker;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public abstract class IntervalValuesMaker<T> implements Maker<T>
{

   public static <T> Maker<T> startFrom(T start, long difference)
   {
      if (Integer.class.isInstance(start))
      {
         return new IntervalIntegerValuesMaker<T>(start, difference);
      }
      if (Long.class.isInstance(start))
      {
         return new IntervalLongValuesMaker<T>(start, difference);
      }
      if (Date.class.isInstance(start))
      {
         return new IntervalDateValuesMaker<T>(start, difference);
      }
      if (String.class.isInstance(start))
      {
         return new IntervalStringValuesMaker<T>((String) start, difference);
      }
      throw new UnsupportedOperationException("only support Number, Date and String type");
   }

   @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
   private static class IntervalIntegerValuesMaker<T> implements Maker<T>
   {
      private final T start;
      private final long difference;
      private Integer current;

      @Override
      public T value()
      {
         next();
         return (T) current;
      }

      private synchronized void next()
      {
         if (current == null)
         {
            current = (Integer) start;
         }
         else
         {
            current = Long.valueOf(current + difference).intValue();
         }
      }
   }

   @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
   private static class IntervalLongValuesMaker<T> implements Maker<T>
   {
      private final T start;
      private final long difference;
      private Long current;

      @Override
      public T value()
      {
         next();
         return (T) current;
      }

      private synchronized void next()
      {
         if (current == null)
         {
            current = (Long) start;
         }
         else
         {
            current = current + difference;
         }
      }
   }

   @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
   private static class IntervalDateValuesMaker<T> implements Maker<T>
   {
      private final T start;
      private final long difference;
      private Date current;

      @Override
      public T value()
      {
         next();
         return (T) current;
      }

      private synchronized void next()
      {
         if (current == null)
         {
            current = (Date) start;
         }
         else
         {
            current = new Date(current.getTime() + difference);
         }
      }
   }

   @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
   private static class IntervalStringValuesMaker<T> implements Maker<T>
   {
      private final String start;
      private final long difference;
      private AtomicLong current = new AtomicLong(0);

      @Override
      public T value()
      {
         return (T) (start + current.addAndGet(difference));
      }

   }
}
