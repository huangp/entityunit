package com.github.huangp.makeit.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.Maker;
import com.github.huangp.makeit.maker.PreferredValueMakersRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */

@NoArgsConstructor(staticName = "builder")
@Slf4j
public class EntityPersistServiceBuilder
{
   private ScanOption scanOption = ScanOption.IgnoreOptionalOneToOne;
   private BeanValueHolder valueHolder = new BeanValueHolder();
   private PreferredValueMakersRegistry registry = new PreferredValueMakersRegistry();

   public EntityPersistServiceBuilder ignoreOptionalOneToOne()
   {
      scanOption = ScanOption.IgnoreOptionalOneToOne;
      return this;
   }

   public EntityPersistServiceBuilder includeOptionalOneToOne()
   {
      scanOption = ScanOption.IncludeOneToOne;
      return this;
   }

   public EntityPersistServiceBuilder reuseObjects(BeanValueHolder beanValueHolder)
   {
      valueHolder.merge(beanValueHolder);
      return this;
   }
   
   public EntityPersistServiceBuilder reuseEntities(Collection<Object> entities)
   {
      for (Object entity : entities)
      {
         Class aClass = entity.getClass();
         valueHolder.putIfNotNull(aClass, entity);
      }
      return this;
   }

   public EntityPersistServiceBuilder reuseEntity(Serializable entity)
   {
      Class aClass = entity.getClass();
      valueHolder.putIfNotNull(aClass, entity);
      return this;
   }

   public EntityPersistServiceBuilder reuseEntities(Object first, Object second, Object... rest)
   {
      List<Object> objects = ImmutableList.builder().add(first).add(second).add(rest).build();
      return reuseEntities(objects);
   }

   public EntityPersistServiceBuilder addFieldOrPropertyMaker(Class<?> ownerType, String fieldName, Maker<?> maker)
   {
      registry.addFieldOrPropertyMaker(ownerType, fieldName, maker);
      return this;
   }

   public EntityPersistServiceBuilder addConstructorParameterMaker(Class<?> ownerType, int argIndex, Maker<?> maker)
   {
      registry.addConstructorParameterMaker(ownerType, argIndex, maker);
      return this;
   }

   public EntityPersistServiceBuilder reusePreferredValueMakers(PreferredValueMakersRegistry other)
   {
      registry.merge(other);
      return this;
   }

   public EntityPersistServiceBuilder mergeContext(MakeContext context)
   {
      valueHolder.merge(context.getBeanValueHolder());
      registry.merge(context.getPreferredValueMakers());
      return this;
   }

   public EntityPersistService build()
   {

      log.debug("registry: {}", registry);
      log.debug("bean value holder: {}", valueHolder);
      EntityClassScanner scanner = new EntityClassScanner(scanOption);
      MakeContext context = new MakeContext(valueHolder, registry);
      return new EntityPersistServiceImpl(scanner, context);
   }
}
