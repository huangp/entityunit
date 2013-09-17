package com.github.huangp.makeit.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.Maker;
import com.github.huangp.makeit.maker.PreferredValueMakersRegistry;
import com.google.common.collect.ImmutableList;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang 
 */

@NoArgsConstructor(staticName = "builder")
@Slf4j
public class EntityPersisterBuilder
{
   private ScanOption scanOption = ScanOption.IgnoreOptionalOneToOne;
   private BeanValueHolder valueHolder = new BeanValueHolder();
   private PreferredValueMakersRegistry registry = new PreferredValueMakersRegistry();

   public EntityPersisterBuilder ignoreOptionalOneToOne()
   {
      scanOption = ScanOption.IgnoreOptionalOneToOne;
      return this;
   }

   public EntityPersisterBuilder includeOptionalOneToOne()
   {
      scanOption = ScanOption.IncludeOneToOne;
      return this;
   }

   public EntityPersisterBuilder reuseObjects(BeanValueHolder beanValueHolder)
   {
      valueHolder.merge(beanValueHolder);
      return this;
   }
   
   public EntityPersisterBuilder reuseEntities(Collection<Object> entities)
   {
      for (Object entity : entities)
      {
         Class aClass = entity.getClass();
         valueHolder.putIfNotNull(aClass, entity);
      }
      return this;
   }

   public EntityPersisterBuilder reuseEntity(Serializable entity)
   {
      Class aClass = entity.getClass();
      valueHolder.putIfNotNull(aClass, entity);
      return this;
   }

   public EntityPersisterBuilder reuseEntities(Object first, Object second, Object... rest)
   {
      List<Object> objects = ImmutableList.builder().add(first).add(second).add(rest).build();
      return reuseEntities(objects);
   }

   public EntityPersisterBuilder addFieldOrPropertyMaker(Class<?> ownerType, String fieldName, Maker<?> maker)
   {
      registry.addFieldOrPropertyMaker(ownerType, fieldName, maker);
      return this;
   }

   public EntityPersisterBuilder addConstructorParameterMaker(Class<?> ownerType, int argIndex, Maker<?> maker)
   {
      registry.addConstructorParameterMaker(ownerType, argIndex, maker);
      return this;
   }

   public EntityPersisterBuilder reusePreferredValueMakers(PreferredValueMakersRegistry other)
   {
      registry.merge(other);
      return this;
   }

   public EntityPersisterBuilder mergeContext(MakeContext context)
   {
      valueHolder.merge(context.getBeanValueHolder());
      registry.merge(context.getPreferredValueMakers());
      return this;
   }

   public EntityPersister build()
   {

      log.debug("registry: {}", registry);
      log.debug("bean value holder: {}", valueHolder);
      EntityClassScanner scanner = new EntityClassScanner(scanOption);
      MakeContext context = new MakeContext(valueHolder, registry);
      return new EntityPersisterImpl(scanner, context);
   }
}
