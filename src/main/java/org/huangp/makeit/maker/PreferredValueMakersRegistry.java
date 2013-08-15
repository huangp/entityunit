package org.huangp.makeit.maker;

import java.util.Map;
import java.util.WeakHashMap;

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
   private static Map<String, Maker<?>> makers = new WeakHashMap<String, Maker<?>>();

   public static PreferredValueMakersRegistry registry()
   {
      return registry;
   }

   public PreferredValueMakersRegistry add(String fullyQualifiedName, Maker<?> maker)
   {
      Preconditions.checkNotNull(fullyQualifiedName);
      Preconditions.checkNotNull(maker);
      makers.put(fullyQualifiedName, maker);
      return this;
   }

   public Optional<Maker<?>> getMaker(Settable settable)
   {
      return Optional.<Maker<?>>fromNullable(makers.get(settable.fullyQualifiedName()));
   }
}
