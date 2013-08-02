package org.huangp.scanner;

import java.lang.reflect.Method;

import org.huangp.util.Settable;

/**         \
 * This class wraps an entity class and use reflection to fetch all interested elements
 * (Field or Method depends on access type).
 *
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface EntityClass
{
   Class getType();

   EntityClassImpl markScanned();

   Iterable<Class<?>> getDependingEntityTypes();

   Iterable<Method> getContainingEntitiesGetterMethods();

   boolean isScanned();

   Iterable<Settable> getElements();
}
