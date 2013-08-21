package com.github.huangp.makeit.entity;

import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.Maker;
import com.github.huangp.makeit.maker.PreferredValueMakersRegistry;

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

   public EntityPersistServiceBuilder addFieldMaker(Class<?> ownerType, String fieldName, Maker<?> maker)
   {
      registry.addFieldOrPropertyMaker(ownerType, fieldName, maker);
      return this;
   }

   public EntityPersistServiceBuilder addConstructorMaker(Class<?> ownerType, int argIndex, Maker<?> maker)
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
