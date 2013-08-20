package org.huangp.makeit.maker;

import java.util.Map;
import java.util.WeakHashMap;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
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

   public PreferredValueMakersRegistry addFieldOrPropertyMaker(Class ownerType, String propertyName, Maker<?> maker)
   {
      Preconditions.checkNotNull(ownerType);
      Preconditions.checkNotNull(propertyName);
      makers.put(Matchers.equalTo(String.format(Settable.FULL_NAME_FORMAT, ownerType.getName(), propertyName)), maker);
      return this;
   }

   public PreferredValueMakersRegistry addConstructorParameterMaker(Class ownerType, int argIndex, Maker<?> maker)
   {
      Preconditions.checkNotNull(ownerType);
      Preconditions.checkArgument(argIndex >= 0);
      makers.put(Matchers.equalTo(String.format(Settable.FULL_NAME_FORMAT, ownerType.getName(), "arg" + argIndex)), maker);
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

   public PreferredValueMakersRegistry clear()
   {
      makers.clear();
      return this;
   }
}
