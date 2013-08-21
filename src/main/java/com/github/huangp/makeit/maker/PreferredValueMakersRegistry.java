package com.github.huangp.makeit.maker;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import com.github.huangp.makeit.util.Settable;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import lombok.NoArgsConstructor;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@NoArgsConstructor
public class PreferredValueMakersRegistry
{

   private Map<Matcher<?>, Maker<?>> makers = new HashMap<Matcher<?>, Maker<?>>();

   public PreferredValueMakersRegistry add(Matcher<?> settableMatcher, Maker<?> maker)
   {
      Preconditions.checkNotNull(settableMatcher);
      Preconditions.checkNotNull(maker);
      makers.put(settableMatcher, maker);
      return this;
   }

   public PreferredValueMakersRegistry merge(PreferredValueMakersRegistry otherRegistry)
   {
      this.makers.putAll(otherRegistry.makers);
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

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this)
            .add("makers", makers)
            .toString();
   }
}
