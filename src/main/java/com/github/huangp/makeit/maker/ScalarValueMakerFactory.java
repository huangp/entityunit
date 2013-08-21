package com.github.huangp.makeit.maker;

import java.lang.reflect.Type;
import java.util.Date;

import com.github.huangp.makeit.entity.MakeContext;
import com.github.huangp.makeit.util.ClassUtil;
import com.github.huangp.makeit.util.Settable;
import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
@RequiredArgsConstructor
public class ScalarValueMakerFactory
{
   private final MakeContext context;

   public Maker from(Settable settable)
   {
      Optional<Maker<?>> makerOptional = context.getPreferredValueMakers().getMaker(settable);
      if (makerOptional.isPresent())
      {
         return makerOptional.get();
      }

      Type type = settable.getType();
      return from(type, Optional.of(settable));
   }

   protected Maker from(Type type, Optional<Settable> optionalAnnotatedElement)
   {
      TypeToken<?> token = TypeToken.of(type);
      if (token.getRawType().isPrimitive())
      {
         return new PrimitiveMaker(token.getRawType());
      }
      if (type == String.class)
      {
         return StringMaker.from(optionalAnnotatedElement);
      }
      if (type == Date.class)
      {
         return new DateMaker();
      }
      if (TypeToken.of(Number.class).isAssignableFrom(type))
      {
         return new NumberMaker();
      }
      if (token.isArray())
      {
         log.debug("array type: {}", token.getComponentType());
         return new NullMaker();
      }
      if (token.getRawType().isEnum())
      {
         log.debug("enum type: {}", type);
         return new EnumMaker(token.getRawType().getEnumConstants());
      }
      if (ClassUtil.isCollection(type))
      {
         log.debug("collection: {}", token);
         return new NullMaker();
      }
      if (ClassUtil.isMap(type))
      {
         log.debug("map: {}", token);
         return new NullMaker();
      }
      if (ClassUtil.isEntity(type))
      {
         log.debug("{} is entity type", token);
         // we don't want to make unnecessary entities
         // @see EntityPersistService
         return new ReuseOrNullMaker(context.getBeanValueHolder(), token.getRawType());
      }
      log.debug("guessing this is a bean {}", token);
      return new BeanMaker(token.getRawType(), context);
   }

}