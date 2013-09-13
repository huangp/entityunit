package com.github.huangp.entities;

import java.io.Serializable;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */

@MappedSuperclass
public class Identifier implements Serializable
{
   @Setter(AccessLevel.PACKAGE)
   protected Long id;

   // TODO works fine in hibernate if @Id is on field. But EntityClass assumes this is property access type
   @Id
   @GeneratedValue
   public Long getId()
   {
      return id;
   }
}
