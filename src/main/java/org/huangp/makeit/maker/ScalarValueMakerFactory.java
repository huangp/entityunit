package org.huangp.makeit.maker;

import java.lang.reflect.Type;
import java.util.Date;

import org.huangp.makeit.holder.BeanValueHolder;
import org.huangp.makeit.holder.BeanValueHolderImpl;
import org.huangp.makeit.scanner.EntityClass;
import org.huangp.makeit.util.ClassUtil;
import org.huangp.makeit.util.Settable;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ScalarValueMakerFactory
{
   private final BeanValueHolder holder;

   static ScalarValueMakerFactory factory(BeanValueHolder holder)
   {
      return new ScalarValueMakerFactory(holder);
   }

   public Maker from(Settable settable)
   {
      Optional<Maker<?>> makerOptional = PreferredValueMakersRegistry.registry().getMaker(settable);
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
         // @see org.huangp.makeit.entity.EntityPersistService
         return new ReuseOrNullMaker(holder, token.getRawType());
      }
      log.debug("guessing this is a bean {}", token);
      return new BeanMaker(token.getRawType(), holder);
   }

}
