package org.huangp.makeit.maker;

import java.util.Map;
import java.util.WeakHashMap;

import org.hamcrest.Matcher;
import org.huangp.makeit.util.Settable;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PreferredValueMakersRegistry
{
   private static PreferredValueMakersRegistry registry = new PreferredValueMakersRegistry();
   private static Map<Matcher<?>, Maker<?>> makers = new WeakHashMap<Matcher<?>, Maker<?>>();

   public static PreferredValueMakersRegistry registry()
   {
      return registry;
   }

   public PreferredValueMakersRegistry add(Matcher<?> settableMatcher, Maker<?> maker)
   {
      Preconditions.checkNotNull(settableMatcher);
      Preconditions.checkNotNull(maker);
      makers.put(settableMatcher, maker);
      return this;
   }

   public Optional<Maker<?>> getMaker(Settable settable)
   {
      for (Matcher<?> matcher : makers.keySet())
      {
         if (matcher.matches(settable.fullyQualifiedName()))
         {
            return Optional.<Maker<?>>of(makers.get(matcher));
         }
      }
      return Optional.absent();
   }
}
