package com.github.huangp.entityunit.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.maker.Maker;
import com.github.huangp.entityunit.maker.PreferredValueMakersRegistry;
import com.google.common.collect.ImmutableList;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang 
 */

@NoArgsConstructor(staticName = "builder")
@Slf4j
public class EntityMakerBuilder
{
   private ScanOption scanOption = ScanOption.IgnoreOptionalOneToOne;
   private BeanValueHolder valueHolder = new BeanValueHolder();
   private PreferredValueMakersRegistry registry = new PreferredValueMakersRegistry();

   public EntityMakerBuilder ignoreOptionalOneToOne()
   {
      scanOption = ScanOption.IgnoreOptionalOneToOne;
      return this;
   }

   public EntityMakerBuilder includeOptionalOneToOne()
   {
      scanOption = ScanOption.IncludeOneToOne;
      return this;
   }

   public EntityMakerBuilder reuseObjects(BeanValueHolder beanValueHolder)
   {
      valueHolder.merge(beanValueHolder);
      return this;
   }
   
   public EntityMakerBuilder reuseEntities(Collection<Object> entities)
   {
      for (Object entity : entities)
      {
         Class aClass = entity.getClass();
         valueHolder.putIfNotNull(aClass, entity);
      }
      return this;
   }

   public EntityMakerBuilder reuseEntity(Serializable entity)
   {
      Class aClass = entity.getClass();
      valueHolder.putIfNotNull(aClass, entity);
      return this;
   }

   public EntityMakerBuilder reuseEntities(Object first, Object second, Object... rest)
   {
      List<Object> objects = ImmutableList.builder().add(first).add(second).add(rest).build();
      return reuseEntities(objects);
   }

   public EntityMakerBuilder addFieldOrPropertyMaker(Class<?> ownerType, String fieldName, Maker<?> maker)
   {
      registry.addFieldOrPropertyMaker(ownerType, fieldName, maker);
      return this;
   }

   public EntityMakerBuilder addConstructorParameterMaker(Class<?> ownerType, int argIndex, Maker<?> maker)
   {
      registry.addConstructorParameterMaker(ownerType, argIndex, maker);
      return this;
   }

   public EntityMakerBuilder reusePreferredValueMakers(PreferredValueMakersRegistry other)
   {
      registry.merge(other);
      return this;
   }

   public EntityMakerBuilder mergeContext(MakeContext context)
   {
      valueHolder.merge(context.getBeanValueHolder());
      registry.merge(context.getPreferredValueMakers());
      return this;
   }

   public EntityMaker build()
   {

      log.debug("registry: {}", registry);
      log.debug("bean value holder: {}", valueHolder);
      EntityClassScanner scanner = new EntityClassScanner(scanOption);
      MakeContext context = new MakeContext(valueHolder, registry);
      return new EntityMakerImpl(scanner, context);
   }
}
