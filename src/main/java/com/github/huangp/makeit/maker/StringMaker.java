package com.github.huangp.makeit.maker;

import java.lang.annotation.Annotation;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.validator.constraints.Email;
import com.github.huangp.makeit.util.Settable;
import com.google.common.base.Optional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class StringMaker implements Maker<String>
{
   public static final int DEFAULT_MAX = 10;
   private final boolean isEmail;
   private final int min;
   private final int max;

   public static StringMaker from(Optional<Settable> optionalElement)
   {
      if (!optionalElement.isPresent())
      {
         return new StringMaker(false, 0, DEFAULT_MAX);
      }

      boolean isEmail = false;
      int min = 0;
      int max = DEFAULT_MAX;
      Settable settable = optionalElement.get();
      for (Annotation annotation : settable.getAnnotations())
      {
         if (annotation instanceof Email)
         {
            isEmail = true;
         }
         if (annotation instanceof Size)
         {
            Size size = (Size) annotation;
            min = size.min();
            if (size.max() != Integer.MAX_VALUE)
            {
               max = size.max();
            }
            else
            {
               max = Math.max(min, DEFAULT_MAX);
            }
         }
         if (annotation instanceof Pattern)
         {
            log.warn("can not auto generate string matches pattern constraint for {}", settable.fullyQualifiedName());
         }
         // TODO Max and Min?
      }
      return new StringMaker(isEmail, min, max);
   }

   @Override
   public String value()
   {
      if (isEmail)
      {
         return RandomStringUtils.randomAlphabetic(5) + "@nowhere.org";
      }
      int length = Math.min(DEFAULT_MAX, this.max);
      return RandomStringUtils.randomAlphabetic(Math.max(length, min));
   }
}
